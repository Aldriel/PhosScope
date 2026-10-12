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

import { useEffect, useState } from 'react';
import { copyOf, type Copy } from '../i18n/copy';
import {
  ApiError,
  createWorkspace,
  downloadReport,
  saveBlob,
  saveSettings,
  uploadTable,
} from '../model/api';
import { formatNumber, negLog10, siteLabel, volcanoX, volcanoY } from '../model/plot';
import {
  initialFont,
  initialLanguage,
  saveFont,
  saveLanguage,
  stepFont,
  type Lang,
} from '../model/preferences';
import type { Analysis, AnalysisSettings, Workspace } from '../model/types';
import { Footer } from './Footer';

const EXAMPLE = 'simulated-tumor';

/** The five steps of the analysis, in both languages. */
export function App() {
  const [language, setLanguage] = useState<Lang>(() => initialLanguage(navigator.language));
  const [font, setFont] = useState(initialFont);
  const [tab, setTab] = useState<'data' | 'cleaning' | 'imputation' | 'results' | 'report'>('data');
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [message, setMessage] = useState('');
  const [help, setHelp] = useState(false);
  const [title, setTitle] = useState('PhosScope');
  const copy = copyOf(language);

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.style.fontSize = `${font}px`;
  }, [language, font]);

  function chooseLanguage(next: Lang) {
    setLanguage(next);
    saveLanguage(next);
  }

  function chooseFont(delta: number) {
    const next = stepFont(font, delta);
    setFont(next);
    saveFont(next);
  }

  async function openExample() {
    await run(async () => setWorkspace(await createWorkspace(EXAMPLE)));
  }

  async function openFile(file: File | undefined) {
    if (!file) {
      return;
    }
    await run(async () => {
      const current = workspace ?? (await createWorkspace());
      setWorkspace(await uploadTable(current.id, file));
    });
  }

  async function apply(settings: AnalysisSettings) {
    if (!workspace) {
      return;
    }
    await run(async () => setWorkspace(await saveSettings(workspace.id, settings)));
  }

  async function report() {
    if (!workspace?.analysis) {
      return;
    }
    try {
      const blob = await downloadReport(workspace.id, language, title);
      saveBlob(blob, `${title || 'PhosScope'}.pdf`);
      setMessage(copy.ready);
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : copy.error);
    }
  }

  async function run(action: () => Promise<void>) {
    setMessage('');
    try {
      await action();
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : copy.error);
    }
  }

  const analysis = workspace?.analysis ?? null;
  return (
    <div className="app">
      <header className="toolbar">
        <h1>{copy.title}</h1>
        <button type="button" onClick={() => chooseLanguage('fr')}>
          {copy.french}
        </button>
        <button type="button" onClick={() => chooseLanguage('en')}>
          {copy.english}
        </button>
        <button type="button" aria-label={copy.smaller} onClick={() => chooseFont(-1)}>
          A−
        </button>
        <button type="button" aria-label={copy.larger} onClick={() => chooseFont(1)}>
          A+
        </button>
        <button type="button" onClick={() => setHelp((open) => !open)}>
          {copy.help}
        </button>
      </header>
      <nav className="tabs">
        {(['data', 'cleaning', 'imputation', 'results', 'report'] as const).map((name) => (
          <button key={name} type="button" aria-pressed={tab === name} onClick={() => setTab(name)}>
            {copy[name]}
          </button>
        ))}
      </nav>
      {help ? <p className="guide">{copy.guide}</p> : null}
      {message ? <p role="status">{message}</p> : null}
      {tab === 'data' ? (
        <DataTab
          copy={copy}
          workspace={workspace}
          onExample={() => void openExample()}
          onFile={(file) => void openFile(file)}
          onGroup={(sample, group) => void apply(withGroup(analysis, sample, group))}
        />
      ) : null}
      {tab === 'cleaning' && analysis ? (
        <CleaningTab copy={copy} analysis={analysis} onApply={(settings) => void apply(settings)} />
      ) : null}
      {tab === 'imputation' && analysis ? (
        <ImputationTab
          copy={copy}
          analysis={analysis}
          onApply={(settings) => void apply(settings)}
        />
      ) : null}
      {tab === 'results' && analysis ? (
        <ResultsTab copy={copy} analysis={analysis} onApply={(settings) => void apply(settings)} />
      ) : null}
      {tab === 'report' ? (
        <section>
          <label>
            {copy.reportTitle}
            <input
              aria-label={copy.reportTitle}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>
          <button type="button" onClick={() => void report()}>
            {copy.build}
          </button>
        </section>
      ) : null}
      <Footer language={language} />
    </div>
  );
}

function withGroup(
  analysis: Analysis | null,
  sample: string,
  group: AnalysisSettings['groups'][string],
): AnalysisSettings {
  const settings = analysis?.settings;
  if (!settings) {
    throw new ApiError('NO_TABLE', 'NO_TABLE');
  }
  return { ...settings, groups: { ...settings.groups, [sample]: group } };
}

function DataTab({
  copy,
  workspace,
  onExample,
  onFile,
  onGroup,
}: {
  copy: Copy;
  workspace: Workspace | null;
  onExample: () => void;
  onFile: (file: File | undefined) => void;
  onGroup: (sample: string, group: AnalysisSettings['groups'][string]) => void;
}) {
  const analysis = workspace?.analysis;
  return (
    <section>
      <p>{copy.intro}</p>
      <button type="button" onClick={onExample}>
        {copy.example}
      </button>
      <label>
        {copy.open}
        <input
          aria-label={copy.open}
          type="file"
          onChange={(event) => onFile(event.target.files?.[0])}
        />
      </label>
      {!analysis ? <p>{copy.empty}</p> : null}
      {workspace?.simulated ? <p>{copy.simulated}</p> : null}
      {analysis ? (
        <>
          <p>
            {copy.file}: {workspace?.fileName} · {copy.format}: {workspace?.format}
          </p>
          <p>{statusText(copy, analysis)}</p>
          <ul>
            {analysis.sampleNames.map((sample) => (
              <li key={sample}>
                <label>
                  {sample}
                  <select
                    aria-label={sample}
                    value={analysis.settings.groups[sample] ?? 'UNASSIGNED'}
                    onChange={(event) =>
                      onGroup(sample, event.target.value as AnalysisSettings['groups'][string])
                    }
                  >
                    <option value="CONTROL">{copy.control}</option>
                    <option value="DISEASE">{copy.disease}</option>
                    <option value="UNASSIGNED">{copy.unassigned}</option>
                  </select>
                </label>
              </li>
            ))}
          </ul>
        </>
      ) : null}
    </section>
  );
}

function CleaningTab({
  copy,
  analysis,
  onApply,
}: {
  copy: Copy;
  analysis: Analysis;
  onApply: (settings: AnalysisSettings) => void;
}) {
  const settings = analysis.settings;
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        onApply({
          ...settings,
          dropContaminants: data.get('contaminants') === 'on',
          dropReverse: data.get('reverse') === 'on',
          dropOnlyBySite: data.get('onlySite') === 'on',
          minLocalization: Number(data.get('localization')),
          minValidPerGroup: Number(data.get('minValid')),
        });
      }}
    >
      <p>
        {copy.cleaningSummary} {analysis.sitesRead}
      </p>
      <label>
        <input name="contaminants" type="checkbox" defaultChecked={settings.dropContaminants} />
        {copy.contaminants}
      </label>
      <label>
        <input name="reverse" type="checkbox" defaultChecked={settings.dropReverse} />
        {copy.reverse}
      </label>
      <label>
        <input name="onlySite" type="checkbox" defaultChecked={settings.dropOnlyBySite} />
        {copy.onlySite}
      </label>
      <label>
        {copy.localization}
        <input
          name="localization"
          aria-label={copy.localization}
          defaultValue={settings.minLocalization}
        />
      </label>
      <label>
        {copy.minValid}
        <input
          name="minValid"
          aria-label={copy.minValid}
          defaultValue={settings.minValidPerGroup}
        />
      </label>
      <button type="submit">{copy.apply}</button>
    </form>
  );
}

function ImputationTab({
  copy,
  analysis,
  onApply,
}: {
  copy: Copy;
  analysis: Analysis;
  onApply: (settings: AnalysisSettings) => void;
}) {
  const settings = analysis.settings;
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        onApply({
          ...settings,
          imputation: String(data.get('method')) as AnalysisSettings['imputation'],
          downshift: Number(data.get('downshift')),
          width: Number(data.get('width')),
          seed: Number(data.get('seed')),
        });
      }}
    >
      <label>
        {copy.method}
        <select name="method" aria-label={copy.method} defaultValue={settings.imputation}>
          <option value="PERSEUS">{copy.perseus}</option>
          <option value="HALF_MINIMUM">{copy.half}</option>
          <option value="NONE">{copy.none}</option>
        </select>
      </label>
      <label>
        {copy.downshift}
        <input name="downshift" aria-label={copy.downshift} defaultValue={settings.downshift} />
      </label>
      <label>
        {copy.width}
        <input name="width" aria-label={copy.width} defaultValue={settings.width} />
      </label>
      <label>
        {copy.seed}
        <input name="seed" aria-label={copy.seed} defaultValue={settings.seed} />
      </label>
      <button type="submit">{copy.apply}</button>
    </form>
  );
}

function ResultsTab({
  copy,
  analysis,
  onApply,
}: {
  copy: Copy;
  analysis: Analysis;
  onApply: (settings: AnalysisSettings) => void;
}) {
  const settings = analysis.settings;
  return (
    <section>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          const data = new FormData(event.currentTarget);
          onApply({
            ...settings,
            fdr: Number(data.get('fdr')),
            minAbsLog2FoldChange: Number(data.get('fold')),
          });
        }}
      >
        <label>
          {copy.fdr}
          <input name="fdr" aria-label={copy.fdr} defaultValue={settings.fdr} />
        </label>
        <label>
          {copy.fold}
          <input name="fold" aria-label={copy.fold} defaultValue={settings.minAbsLog2FoldChange} />
        </label>
        <button type="submit">{copy.apply}</button>
      </form>
      <p>
        {analysis.sites.length} {copy.summary} {countSignificant(analysis)}. {copy.cleaningSummary}{' '}
        {analysis.sitesRead}
      </p>
      <svg className="volcano" viewBox="0 0 500 260" role="img">
        {analysis.sites.map((site) => (
          <circle
            key={`${site.gene}-${site.position}`}
            cx={volcanoX(site.log2FoldChange, 500)}
            cy={volcanoY(negLog10(site.pValue), 260)}
            r={4}
            className={pointClass(site.log2FoldChange, site.significant)}
          >
            <title>{site.gene}</title>
          </circle>
        ))}
      </svg>
      <table>
        <thead>
          <tr>
            <th>{copy.gene}</th>
            <th>{copy.site}</th>
            <th>log2 FC</th>
            <th>p</th>
            <th>q</th>
            <th>{copy.call}</th>
          </tr>
        </thead>
        <tbody>
          {analysis.sites.map((site) => (
            <tr key={`${site.protein}-${site.position}`}>
              <td>{site.gene}</td>
              <td>{siteLabel(site.aminoAcid, site.position)}</td>
              <td>{formatNumber(site.log2FoldChange)}</td>
              <td>{formatNumber(site.pValue)}</td>
              <td>{formatNumber(site.qValue)}</td>
              <td>{callOf(copy, site.log2FoldChange, site.significant)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function pointClass(fold: number, significant: boolean): string {
  if (!significant) {
    return 'point-none';
  }
  return fold >= 0 ? 'point-up' : 'point-down';
}

function callOf(copy: Copy, fold: number, significant: boolean): string {
  if (!significant) {
    return copy.no;
  }
  return fold >= 0 ? copy.up : copy.down;
}

function statusText(copy: Copy, analysis: Analysis): string {
  if (analysis.status === 'NEED_GROUPS') {
    return copy.needGroups;
  }
  if (analysis.status === 'NEED_REPLICATES') {
    return copy.needReplicates;
  }
  if (analysis.status === 'NO_SITE') {
    return copy.noSite;
  }
  return `${countSignificant(analysis)} ${copy.summary}`;
}

function countSignificant(analysis: Analysis): number {
  return analysis.sites.filter((site) => site.significant).length;
}
