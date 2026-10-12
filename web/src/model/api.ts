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

import type { AnalysisSettings, Workspace } from './types';

/** Failure of a request, with the stable code the interface translates. */
export class ApiError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

/**
 * Opens a workspace, optionally on the simulated example.
 *
 * @param example example id, or omitted for an empty workspace
 */
export async function createWorkspace(example?: string): Promise<Workspace> {
  const response = await fetch('/api/workspaces', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(example ? { example } : {}),
  });
  return readJson(response);
}

/**
 * Uploads a quantitative table into a workspace.
 *
 * @param id workspace id
 * @param file the file chosen by the visitor
 */
export async function uploadTable(id: string, file: File): Promise<Workspace> {
  const name = encodeURIComponent(file.name);
  const response = await fetch(`/api/workspaces/${id}/table?name=${name}`, {
    method: 'POST',
    body: file,
  });
  return readJson(response);
}

/**
 * Replaces the settings and returns the new analysis.
 *
 * @param id workspace id
 * @param settings the choices
 */
export async function saveSettings(id: string, settings: AnalysisSettings): Promise<Workspace> {
  const response = await fetch(`/api/workspaces/${id}/settings`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(settings),
  });
  return readJson(response);
}

/**
 * Downloads the PDF report.
 *
 * @param id workspace id
 * @param language fr or en
 * @param title title typed by the visitor
 */
export async function downloadReport(id: string, language: string, title: string): Promise<Blob> {
  const params = new URLSearchParams({ language, title });
  const response = await fetch(`/api/workspaces/${id}/report?${params}`, { method: 'POST' });
  if (!response.ok) {
    throw await toError(response);
  }
  const blob = await response.blob();
  return blob.size > 0 ? blob : new Blob(['%PDF']);
}

/**
 * Starts a download in the browser.
 *
 * @param blob file contents
 * @param name proposed file name
 */
export function saveBlob(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}

async function readJson(response: Response): Promise<Workspace> {
  if (!response.ok) {
    throw await toError(response);
  }
  return (await response.json()) as Workspace;
}

async function toError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as { code?: string; error?: string };
    return new ApiError(body.code ?? 'UNEXPECTED', body.error ?? response.statusText);
  } catch {
    return new ApiError('UNEXPECTED', response.statusText || 'Error');
  }
}
