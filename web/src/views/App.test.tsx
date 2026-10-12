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

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Workspace } from '../model/types';
import { App } from './App';

const workspace: Workspace = {
  id: 'w1',
  fileName: 'Phospho (STY)Sites.txt',
  format: 'MAXQUANT',
  simulated: true,
  analysis: {
    fileName: 'Phospho (STY)Sites.txt',
    format: 'MAXQUANT',
    sampleNames: ['Control_1', 'Disease_1'],
    settings: {
      groups: { Control_1: 'CONTROL', Disease_1: 'DISEASE' },
      dropContaminants: true,
      dropReverse: true,
      dropOnlyBySite: true,
      minLocalization: 0.75,
      minValidPerGroup: 2,
      imputation: 'PERSEUS',
      downshift: 1.8,
      width: 0.3,
      seed: 1,
      fdr: 0.05,
      minAbsLog2FoldChange: 1,
    },
    sitesRead: 3,
    droppedContaminant: 1,
    droppedReverse: 0,
    droppedOnlyBySite: 0,
    droppedLocalization: 0,
    droppedMissing: 0,
    imputedValues: 1,
    status: 'OK',
    sites: [
      {
        gene: 'AKT1',
        protein: 'P31749',
        position: '473',
        aminoAcid: 'S',
        log2FoldChange: 2,
        pValue: 0.001,
        qValue: 0.01,
        significant: true,
      },
      {
        gene: 'EGFR',
        protein: 'P00533',
        position: '1173',
        aminoAcid: 'Y',
        log2FoldChange: -2,
        pValue: 0,
        qValue: 0.01,
        significant: true,
      },
    ],
  },
};

beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string) => {
      if (String(url).includes('/report')) {
        return new Response(new Blob(['%PDF']), { status: 200 });
      }
      if (String(url).includes('missing')) {
        return Response.json({ code: 'NO_COLUMNS', error: 'missing columns' }, { status: 400 });
      }
      return Response.json(workspace);
    }),
  );
});

describe('App', () => {
  it('opens the example, switches language and builds a report', async () => {
    localStorage.setItem('phosscope-language', 'fr');
    const user = userEvent.setup();
    render(<App />);
    expect(screen.getByText('Aucun tableau pour l’instant.')).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Ouvrir l’exemple simulé' }));
    expect(await screen.findByText(/simulées/)).toBeTruthy();
    await user.selectOptions(screen.getByLabelText('Disease_1'), 'CONTROL');
    await user.click(screen.getByRole('button', { name: 'Anglais' }));
    expect(screen.getByRole('button', { name: 'Open the simulated example' })).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Larger text' }));
    expect(document.documentElement.style.fontSize).toBe('16px');
    await user.click(screen.getByRole('button', { name: 'Smaller text' }));
    await user.click(screen.getByRole('button', { name: 'User guide' }));
    expect(screen.getByText(/Welch/)).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Results' }));
    expect(screen.getByRole('cell', { name: 'AKT1' })).toBeTruthy();
    expect(screen.getByRole('cell', { name: 'EGFR' })).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Cleaning' }));
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    await user.click(screen.getByRole('button', { name: 'Imputation' }));
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    await user.click(screen.getByRole('button', { name: 'Report' }));
    await user.clear(screen.getByLabelText('Report title'));
    await user.click(screen.getByRole('button', { name: 'Build the PDF report' }));
    expect(await screen.findByText('The report is ready.')).toBeTruthy();
  });

  it('shows an upload error', async () => {
    localStorage.setItem('phosscope-language', 'fr');
    const user = userEvent.setup();
    render(<App />);
    const input = screen.getByLabelText('Ouvrir un tableau');
    const file = new File(['x'], 'missing.txt');
    await user.upload(input, file);
    expect(await screen.findByText('missing columns')).toBeTruthy();
  });

  it('explains an analysis that cannot be tested', async () => {
    localStorage.setItem('phosscope-language', 'en');
    const statuses = ['NEED_GROUPS', 'NEED_REPLICATES', 'NO_SITE', 'OK'] as const;
    const messages = [
      'Assign at least one control sample and one disease sample.',
      'Each group needs at least two samples.',
      'No site can be tested with these filters.',
    ];
    let call = 0;
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        const status = statuses[Math.min(call, statuses.length - 1)];
        call += 1;
        const unchanged = workspace.analysis?.sites[0];
        const sites =
          status === 'OK' && unchanged
            ? [{ ...unchanged, significant: false, log2FoldChange: 0.1 }]
            : workspace.analysis?.sites;
        return Response.json({
          ...workspace,
          analysis: workspace.analysis ? { ...workspace.analysis, status, sites } : null,
        });
      }),
    );
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('button', { name: 'Report' }));
    await user.click(screen.getByRole('button', { name: 'Build the PDF report' }));
    for (const message of messages) {
      await user.click(screen.getByRole('button', { name: 'Data' }));
      await user.click(screen.getByRole('button', { name: 'Open the simulated example' }));
      expect(await screen.findByText(message)).toBeTruthy();
    }
    await user.click(screen.getByRole('button', { name: 'Open the simulated example' }));
    await user.click(screen.getByRole('button', { name: 'Results' }));
    expect(screen.getByRole('cell', { name: 'no' })).toBeTruthy();
  });
});
