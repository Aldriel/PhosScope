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

import { expect, test } from '@playwright/test';

/**
 * A visitor opens the simulated cohort, reads a planted site and downloads the report.
 * The browser language selects French or English.
 */
test('the simulated cohort can be analysed', async ({ page, locale }) => {
  const french = (locale ?? '').toLowerCase().startsWith('fr');
  await page.goto('/');
  await page
    .getByRole('button', {
      name: french ? 'Ouvrir l’exemple simulé' : 'Open the simulated example',
    })
    .click();
  await page.getByRole('button', { name: french ? 'Résultats' : 'Results' }).click();
  await expect(page.getByRole('cell', { name: 'AKT1' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'EGFR' })).toBeVisible();
  await page.getByRole('button', { name: french ? 'Guide d’utilisation' : 'User guide' }).click();
  await expect(page.getByText(/Welch|Welch/)).toBeVisible();
  await page.getByRole('button', { name: french ? 'Texte plus grand' : 'Larger text' }).click();
  await page.getByRole('button', { name: french ? 'Rapport' : 'Report' }).click();
  const download = page.waitForEvent('download');
  await page
    .getByRole('button', { name: french ? 'Produire le rapport PDF' : 'Build the PDF report' })
    .click();
  expect((await download).suggestedFilename()).toContain('.pdf');
  await expect(page.getByRole('link', { name: /Ko-fi/ })).toBeVisible();
  await expect(page.getByRole('link', { name: 'contact@maximeethier.com' })).toBeVisible();
});
