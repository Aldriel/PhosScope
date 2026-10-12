/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  createWorkspace,
  downloadReport,
  saveBlob,
  saveSettings,
  uploadTable,
} from './api';
import { formatNumber, negLog10, siteLabel, volcanoX, volcanoY } from './plot';
import { initialFont, initialLanguage, saveFont, saveLanguage, stepFont } from './preferences';
import type { AnalysisSettings } from './types';

describe('plot', () => {
  it('formats numbers and places volcano points', () => {
    expect(formatNumber(Number.NaN)).toBe('');
    expect(formatNumber(1.2345)).toBe('1.235');
    expect(siteLabel('S', '473')).toBe('S473');
    expect(siteLabel('S', 'S473')).toBe('S473');
    expect(siteLabel('', '15')).toBe('15');
    expect(negLog10(0)).toBe(20);
    expect(negLog10(0.01)).toBeCloseTo(2);
    expect(volcanoX(0, 500)).toBe(250);
    expect(volcanoX(100, 500)).toBe(500);
    expect(volcanoY(0, 200)).toBe(200);
    expect(volcanoY(100, 200)).toBe(0);
  });
});

describe('preferences', () => {
  it('remembers language and steps the text size', () => {
    expect(initialLanguage('fr-CA')).toBe('fr');
    expect(initialLanguage('en-CA')).toBe('en');
    saveLanguage('fr');
    expect(initialLanguage('en')).toBe('fr');
    expect(initialFont()).toBe(13);
    saveFont(16);
    expect(initialFont()).toBe(16);
    saveFont(12);
    expect(initialFont()).toBe(13);
    expect(stepFont(11, -1)).toBe(11);
    expect(stepFont(20, 1)).toBe(20);
    expect(stepFont(13, 1)).toBe(16);
    expect(stepFont(99, -1)).toBe(11);
  });
});

describe('api', () => {
  it('reads a workspace and a report', async () => {
    const workspace = {
      id: 'w',
      fileName: 'a.txt',
      format: 'MAXQUANT',
      simulated: false,
      analysis: null,
    };
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (String(url).endsWith('/report?language=en&title=T')) {
          return new Response(new Blob(['pdf']), { status: 200 });
        }
        if (init?.method === 'PUT') {
          return Response.json({ ...workspace, fileName: 'saved' });
        }
        if (String(url).includes('/table')) {
          return Response.json({ ...workspace, fileName: 'upload.txt' });
        }
        return Response.json(workspace);
      }),
    );
    expect((await createWorkspace('simulated-tumor')).id).toBe('w');
    expect((await createWorkspace()).id).toBe('w');
    const file = new File(['a'], 'upload.txt');
    expect((await uploadTable('w', file)).fileName).toBe('upload.txt');
    const settings = { groups: {} } as AnalysisSettings;
    expect((await saveSettings('w', settings)).fileName).toBe('saved');
    const blob = await downloadReport('w', 'en', 'T');
    expect(blob.size).toBeGreaterThan(0);
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(null, { status: 200 })),
    );
    expect((await downloadReport('w', 'en', '')).size).toBeGreaterThan(0);
    saveBlob(blob, 'PhosScope.pdf');
    expect(URL.createObjectURL).toHaveBeenCalled();
  });

  it('translates an error body and a body that is not JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Response.json({ code: 'NO_COLUMNS', error: 'missing' }, { status: 400 })),
    );
    await expect(createWorkspace()).rejects.toBeInstanceOf(ApiError);
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('nope', { status: 500, statusText: 'Boom' })),
    );
    await expect(createWorkspace()).rejects.toMatchObject({ code: 'UNEXPECTED' });
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Response.json({}, { status: 400 })),
    );
    await expect(createWorkspace()).rejects.toMatchObject({ code: 'UNEXPECTED' });
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('', { status: 502 })),
    );
    await expect(downloadReport('w', 'fr', '')).rejects.toMatchObject({ code: 'UNEXPECTED' });
  });
});
