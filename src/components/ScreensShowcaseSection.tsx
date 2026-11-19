import React, { useState } from 'react';
import { ArrowLeft, ArrowRight, Search } from 'lucide-react';
import { BUSINESS_TYPES, SCREEN_GROUPS, SCREENS_41 } from '../data/daftarSpec';
import {
  BusinessTypeId,
  Language,
  ScreenFlowGroup,
  ScreenId,
  ThemeMode,
  UIStateMode,
} from '../types/daftar';
import { PhonePrototypeFrame } from './PhonePrototypeFrame';

interface ScreensShowcaseSectionProps {
  lang: Language;
  theme: ThemeMode;
  activeScreen: ScreenId;
  activeBusinessType: BusinessTypeId;
  uiState: UIStateMode;
  onChangeUiState: (state: UIStateMode) => void;
  onNavigate: (screen: ScreenId) => void;
  onBack: () => void;
  onSelectBusinessType: (type: BusinessTypeId) => void;
  onToggleLang: () => void;
  onToggleTheme: () => void;
}

export const ScreensShowcaseSection: React.FC<ScreensShowcaseSectionProps> = ({
  lang,
  theme,
  activeScreen,
  activeBusinessType,
  uiState,
  onChangeUiState,
  onNavigate,
  onBack,
  onSelectBusinessType,
  onToggleLang,
  onToggleTheme,
}) => {
  const isAr = lang === 'ar';
  const [groupFilter, setGroupFilter] = useState<'all' | ScreenFlowGroup>('all');
  const [query, setQuery] = useState<string>('');
  const ArrowIcon = isAr ? ArrowLeft : ArrowRight;

  const currentSpec = SCREENS_41.find((s) => s.id === activeScreen) || SCREENS_41[0];
  const currentBusiness =
    BUSINESS_TYPES.find((b) => b.id === activeBusinessType) || BUSINESS_TYPES[0];

  const filteredScreens = SCREENS_41.filter((s) => {
    const matchesGroup = groupFilter === 'all' || s.group === groupFilter;
    const q = query.trim().toLowerCase();
    const matchesQuery =
      !q ||
      s.title.ar.toLowerCase().includes(q) ||
      s.title.en.toLowerCase().includes(q) ||
      s.composeRoute.toLowerCase().includes(q);
    return matchesGroup && matchesQuery;
  });

  const uiStates: { id: UIStateMode; label: string }[] = [
    { id: 'populated', label: isAr ? 'مكتملة' : 'Populated' },
    { id: 'loading', label: isAr ? 'تحميل' : 'Loading' },
    { id: 'empty', label: isAr ? 'فارغة' : 'Empty' },
    { id: 'success', label: isAr ? 'نجاح' : 'Success' },
    { id: 'error', label: isAr ? 'خطأ' : 'Error' },
  ];

  return (
    <section
      id="screens-showcase"
      className="py-16 border-t border-slate-200 dark:border-slate-800/80"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 mb-8">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold tracking-wide text-[#9A7222] dark:text-[#D9B15B] mb-2">
              {isAr
                ? '03. الشاشات الأساسية والمتخصصة (41 شاشة تفاعلية)'
                : '03. Complete Core & Specialized Screens (41 Interactive Screens)'}
            </p>
            <h2 className="text-2xl sm:text-3xl font-bold text-[#0B192C] dark:text-slate-100 leading-tight [text-wrap:balance]">
              {isAr
                ? 'معاينة شاملة لكافة الشاشات وحالات الواجهة وعزل الأنشطة'
                : 'Complete Screen Suite with UI States & Isolated Activity Switching'}
            </h2>
          </div>

          {/* Isolated Activity Selector */}
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs text-slate-500 dark:text-slate-400">
              {isAr ? 'النشاط المستقل:' : 'Isolated Vault:'}
            </span>
            <select
              value={activeBusinessType}
              onChange={(e) => onSelectBusinessType(e.target.value as BusinessTypeId)}
              className="min-h-[40px] px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0F1E33] text-xs font-semibold text-[#0B192C] dark:text-slate-100"
            >
              {BUSINESS_TYPES.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.activityName[lang]} ({b.name[lang]} · {b.defaultCurrency})
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* UI States & Search Bar */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6 p-4 rounded-2xl bg-white dark:bg-[#0F1E33] border border-slate-200 dark:border-slate-800">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-bold text-slate-500 dark:text-slate-400 me-1">
              {isAr ? 'حالة الواجهة:' : 'UI State:'}
            </span>
            {uiStates.map((st) => (
              <button
                key={st.id}
                type="button"
                onClick={() => onChangeUiState(st.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap ${
                  uiState === st.id
                    ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]'
                    : 'bg-slate-100 dark:bg-[#0B192C] text-slate-600 dark:text-slate-300'
                }`}
              >
                {st.label}
              </button>
            ))}
          </div>

          <div className="relative w-full md:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute top-1/2 -translate-y-1/2 start-3 pointer-events-none" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={isAr ? 'ابحث في الشاشات الـ 41...' : 'Search 41 screens...'}
              className="w-full min-h-[38px] ps-9 pe-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-[#0B192C] text-xs text-[#0B192C] dark:text-slate-100 focus:outline-none focus:border-[#C89D42]"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column: 41-Screen Directory + Active Spec */}
          <div className="lg:col-span-7 space-y-5">
            {/* 6 Group Filter Tabs */}
            <div className="flex flex-wrap gap-1.5 p-1.5 rounded-xl bg-slate-100 dark:bg-[#0F1E33] border border-slate-200 dark:border-slate-800">
              <button
                type="button"
                onClick={() => setGroupFilter('all')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap ${
                  groupFilter === 'all'
                    ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]'
                    : 'text-slate-600 dark:text-slate-300'
                }`}
              >
                {isAr ? 'الكل (41)' : 'All (41)'}
              </button>
              {SCREEN_GROUPS.map((grp) => (
                <button
                  key={grp.id}
                  type="button"
                  onClick={() => setGroupFilter(grp.id)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap ${
                    groupFilter === grp.id
                      ? 'bg-[#0B192C] text-white dark:bg-[#C89D42] dark:text-[#0B192C]'
                      : 'text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {grp.label[lang]}
                </button>
              ))}
            </div>

            {/* Screens List */}
            {filteredScreens.length === 0 ? (
              <div className="p-8 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] text-center space-y-2">
                <p className="text-sm font-bold">{isAr ? 'لا توجد شاشة مطابقة للبحث' : 'No matching screens'}</p>
                <button
                  type="button"
                  onClick={() => {
                    setQuery('');
                    setGroupFilter('all');
                  }}
                  className="text-xs font-bold text-[#9A7222] dark:text-[#D9B15B]"
                >
                  {isAr ? 'إعادة ضبط الفلتر' : 'Reset Filter'}
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-h-[430px] overflow-y-auto pe-1 daftar-scroll">
                {filteredScreens.map((screen) => {
                  const isActive = screen.id === activeScreen;
                  const sectorInactive =
                    screen.sectorOnly && !screen.sectorOnly.includes(activeBusinessType);
                  return (
                    <button
                      key={screen.id}
                      type="button"
                      onClick={() => onNavigate(screen.id)}
                      className={`p-3 rounded-xl border text-start transition-colors flex flex-col justify-between gap-1 ${
                        isActive
                          ? 'border-[#C89D42] bg-[#0B192C] text-white'
                          : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0F1E33] hover:border-[#C89D42]/60'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2 min-w-0">
                          <span
                            className={`font-mono-num text-xs font-bold shrink-0 ${
                              isActive ? 'text-[#D9B15B]' : 'text-[#9A7222] dark:text-[#D9B15B]'
                            }`}
                          >
                            {String(screen.order).padStart(2, '0')}
                          </span>
                          <span className="text-xs font-bold truncate">{screen.title[lang]}</span>
                        </div>
                        {sectorInactive && (
                          <span className="text-[10px] opacity-65 shrink-0">
                            {isAr ? 'قطاع آخر' : 'Other sector'}
                          </span>
                        )}
                      </div>
                      <p
                        className={`text-[11px] truncate ${
                          isActive ? 'text-slate-300' : 'text-slate-500 dark:text-slate-400'
                        }`}
                      >
                        {screen.subtitle[lang]}
                      </p>
                    </button>
                  );
                })}
              </div>
            )}

            {/* Active Screen Technical Specification */}
            <div className="bg-white dark:bg-[#0F1E33] rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-slate-100 dark:border-slate-800">
                <div>
                  <span className="font-mono-num text-xs text-[#9A7222] dark:text-[#D9B15B]">
                    {currentSpec.composeRoute} · {currentBusiness.dbFileName}
                  </span>
                  <h3 className="text-lg font-bold text-[#0B192C] dark:text-slate-100 mt-0.5">
                    {String(currentSpec.order).padStart(2, '0')}. {currentSpec.title[lang]}
                  </h3>
                </div>
                {currentSpec.moduleNumbers.length > 0 && (
                  <span className="font-mono-num text-xs text-slate-500 dark:text-slate-400">
                    {isAr ? 'الوحدات:' : 'Modules:'}{' '}
                    {currentSpec.moduleNumbers.map((n) => `#${n}`).join(', ')}
                  </span>
                )}
              </div>

              <p className="text-sm text-slate-700 dark:text-slate-300 leading-relaxed">
                {currentSpec.purpose[lang]}
              </p>

              <ul className="space-y-1 text-xs text-slate-600 dark:text-slate-400">
                {currentSpec.keyElements[lang].map((el, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="font-mono-num text-[#C89D42] font-bold">·</span>
                    <span>{el}</span>
                  </li>
                ))}
              </ul>

              <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex flex-wrap items-center gap-2">
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                  {isAr ? 'التنقل المباشر:' : 'Direct Routes:'}
                </span>
                {currentSpec.nextScreens.map((nextId) => {
                  const target = SCREENS_41.find((s) => s.id === nextId);
                  if (!target) return null;
                  return (
                    <button
                      key={nextId}
                      type="button"
                      onClick={() => onNavigate(nextId)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-xs font-semibold text-[#0B192C] dark:text-slate-200 hover:border-[#C89D42] transition-colors"
                    >
                      <span>{target.title[lang]}</span>
                      <ArrowIcon className="w-3.5 h-3.5 text-[#C89D42]" />
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Right Column: Live Android Prototype */}
          <div className="lg:col-span-5 lg:sticky lg:top-20">
            <PhonePrototypeFrame
              lang={lang}
              theme={theme}
              activeScreen={activeScreen}
              activeBusinessType={activeBusinessType}
              uiState={uiState}
              onChangeUiState={onChangeUiState}
              onNavigate={onNavigate}
              onBack={onBack}
              onSelectBusinessType={onSelectBusinessType}
              onToggleLang={onToggleLang}
              onToggleTheme={onToggleTheme}
            />
          </div>
        </div>
      </div>
    </section>
  );
};
