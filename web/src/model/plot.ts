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

/** Largest absolute log2 fold change drawn inside the volcano plot. */
export const MAX_FOLD = 4;

/** Largest -log10(p) drawn inside the volcano plot. */
export const MAX_HEIGHT = 8;

/**
 * Formats a number for a table. A non-finite value is left blank.
 *
 * @param value the number
 */
export function formatNumber(value: number): string {
  if (!Number.isFinite(value)) {
    return '';
  }
  return String(Math.round(value * 1000) / 1000);
}

/**
 * Label of a modified residue, such as S473.
 *
 * @param aminoAcid one-letter residue
 * @param position position text from the file
 */
export function siteLabel(aminoAcid: string, position: string): string {
  if (!aminoAcid || position.startsWith(aminoAcid)) {
    return position;
  }
  return aminoAcid + position;
}

/**
 * Height of a volcano point. A p-value of zero is drawn at 20.
 *
 * @param pValue two-sided p-value
 */
export function negLog10(pValue: number): number {
  if (!(pValue > 0) || !Number.isFinite(pValue)) {
    return 20;
  }
  return Math.min(20, -Math.log10(pValue));
}

/**
 * Horizontal position of a volcano point.
 *
 * @param log2FoldChange fold change
 * @param width plot width
 */
export function volcanoX(log2FoldChange: number, width: number): number {
  const limited = Math.min(MAX_FOLD, Math.max(-MAX_FOLD, log2FoldChange));
  return ((limited + MAX_FOLD) / (2 * MAX_FOLD)) * width;
}

/**
 * Vertical position of a volcano point, from the top.
 *
 * @param negLog10P -log10 of the p-value
 * @param height plot height
 */
export function volcanoY(negLog10P: number, height: number): number {
  const limited = Math.min(MAX_HEIGHT, Math.max(0, negLog10P));
  return height - (limited / MAX_HEIGHT) * height;
}
