import React, { useState } from 'react';
import { ArrowLeft, ArrowRight, CheckCircle2, Lock } from 'lucide-react';
import { BUSINESS_TYPES, MODULES_37, NAVIGATION_FLOWS, SCREENS_41 } from '../data/daftarSpec';
import { BusinessTypeId, Language, ScreenId } from '../types/daftar';

interface NavigationMapSectionProps {
  lang: Language;
  activeScreen: ScreenId;
  activeBusinessType: BusinessTypeId;
  onJumpToScreen: (screen: ScreenId) => void;
  onSelectBusinessType: (type: BusinessTypeId) => void;
}

export const NavigationMapSection: React.FC<NavigationMapSectionProps> = ({
  lang,
  activeScreen,
  activeBusinessType,
  onJumpToScreen,
  onSelectBusinessType,
}) => {
  const isAr = lang === 'ar';
  const FlowArrow = isAr ? ArrowLeft : ArrowRight;
  const [layerFilter, setLayerFilter] = useState<
    'all' | 'core' | 'operations' | 'specialized' | 'governance'
  >('all');

  const filteredModules = MODULES_37.filter((m) =>
    layerFilter === 'all' ? true : m.layer === layerFilter
  );

  const isModuleEnabled = (alwaysEnabled: boolean, types?: BusinessTypeId[]) => {
    if (alwaysEnabled) return true;
    return types ? types.includes(activeBusinessType) : false;
  };

  const activeCount = MODULES_37.filter((m) =>
    isModuleEnabled(m.alwaysEnabled, m.businessTypes)
  ).length;

  return (
    <>
      {/* Section 04: Screen Navigation Map */}
      <section
        id="navigation-map"
        className="py-16 border-t border-slate-200 dark:border-slate-800/80"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="max-w-3xl mb-10">
            <p className="text-xs font-semibold tracking-wide text-[#9A7222] dark:text-[#D9B15B] mb-2">
              {isAr
                ? '04. خريطة التنقل الشاملة بين الشاشات الـ 41'
                : '04. Complete 41-Screen Navigation Map'}
            </p>
            <h2 className="text-2xl sm:text-3xl font-bold text-[#0B192C] dark:text-slate-100 leading-tight [text-wrap:balance]">
              {isAr
                ? 'ستة مسارات تنقل متكاملة تغطي التهيئة والمحاسبة والتجارة والأنشطة المتخصصة'
                : 'Six Structured Navigation Flows Covering Setup, Accounting, Commerce & Specialized Sectors'}
            </h2>
          </div>

          <div className="space-y-4">
            {NAVIGATION_FLOWS.map((flow, idx) => (
              <div
                key={flow.id}
                className="bg-white dark:bg-[#0F1E33] rounded-2xl border border-slate-200 dark:border-slate-800 p-5 sm:p-6"
              >
                <div className="mb-4">
                  <div className="flex items-baseline gap-2">
                    <span className="font-mono-num text-xs font-bold text-[#9A7222] dark:text-[#D9B15B]">
                      0{idx + 1}.
                    </span>
                    <h3 className="text-base font-bold text-[#0B192C] dark:text-slate-100">
                      {flow.title[lang]}
                    </h3>
                  </div>
                  <p className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                    {flow.description[lang]}
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {flow.path.map((screenId, stepIdx) => {
                    const spec = SCREENS_41.find((s) => s.id === screenId);
                    if (!spec) return null;
                    const isCurrent = activeScreen === screenId;
                    return (
                      <React.Fragment key={screenId}>
                        <button
                          type="button"
                          onClick={() => onJumpToScreen(screenId)}
                          className={`min-h-[44px] px-3.5 py-2 rounded-xl border text-start transition-colors ${
                            isCurrent
                              ? 'bg-[#0B192C] text-white border-[#C89D42] dark:bg-[#C89D42] dark:text-[#0B192C]'
                              : 'bg-slate-50 dark:bg-[#0B192C] border-slate-200 dark:border-slate-700 hover:border-[#C89D42]'
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <span
                              className={`font-mono-num text-[11px] font-bold ${
                                isCurrent
                                  ? 'text-[#D9B15B] dark:text-[#0B192C]'
                                  : 'text-[#9A7222] dark:text-[#D9B15B]'
                              }`}
                            >
                              {String(spec.order).padStart(2, '0')}
                            </span>
                            <span className="text-xs font-bold whitespace-nowrap">
                              {spec.title[lang]}
                            </span>
                          </div>
                        </button>
                        {stepIdx < flow.path.length - 1 && (
                          <FlowArrow className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        )}
                      </React.Fragment>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Section 05: Modular 37-Module Architecture */}
      <section
        id="modules-architecture"
        className="py-16 border-t border-slate-200 dark:border-slate-800/80"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 mb-8">
            <div className="max-w-3xl">
              <p className="text-xs font-semibold tracking-wide text-[#9A7222] dark:text-[#D9B15B] mb-2">
                {isAr
                  ? '05. تغطية الوحدات الـ 37 وعزل الأنشطة'
                  : '05. Full 37-Module Coverage & Activity Isolation'}
              </p>
              <h2 className="text-2xl sm:text-3xl font-bold text-[#0B192C] dark:text-slate-100 leading-tight [text-wrap:balance]">
                {isAr
                  ? 'كل وحدة من الوحدات الـ 37 مربوطة بشاشتها المستقلة ومفعّلة حسب القطاع'
                  : 'All 37 Modules Mapped to Dedicated Screens & Scoped Per Isolated Business'}
              </h2>
            </div>
            <div className="text-sm font-mono-num font-bold text-[#0B192C] dark:text-[#D9B15B]">
              {isAr
                ? `تغطية الوحدات: 37 / 37 · المفعّل للقطاع: ${activeCount}`
                : `Total Covered: 37 / 37 · Active for Sector: ${activeCount}`}
            </div>
          </div>

          <div className="flex flex-wrap gap-2 mb-6">
            {BUSINESS_TYPES.map((type) => {
              const selected = type.id === activeBusinessType;
              return (
                <button
                  key={type.id}
                  type="button"
                  onClick={() => onSelectBusinessType(type.id)}
                  className={`min-h-[40px] px-4 py-2 rounded-xl border text-xs font-bold transition-colors whitespace-nowrap ${
                    selected
                      ? 'bg-[#0B192C] text-white border-[#0B192C] dark:bg-[#C89D42] dark:text-[#0B192C] dark:border-[#C89D42]'
                      : 'bg-white dark:bg-[#0F1E33] text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-800 hover:border-[#C89D42]'
                  }`}
                >
                  {type.name[lang]}
                </button>
              );
            })}
          </div>

          <div className="flex flex-wrap items-center gap-1.5 p-1.5 rounded-xl bg-slate-100 dark:bg-[#0F1E33] border border-slate-200 dark:border-slate-800 mb-6 w-fit">
            {(
              [
                ['all', isAr ? 'جميع الوحدات (37)' : 'All (37)'],
                ['core', isAr ? 'المحاسبة الأساسية' : 'Core Ledger'],
                ['operations', isAr ? 'العمليات والتجارة' : 'Operations'],
                ['specialized', isAr ? 'الوحدات القطاعية المتخصصة' : 'Specialized'],
                ['governance', isAr ? 'الحماية والذكاء المحلي' : 'Security & AI'],
              ] as const
            ).map(([k, label]) => (
              <button
                key={k}
                type="button"
                onClick={() => setLayerFilter(k)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap ${
                  layerFilter === k
                    ? 'bg-white dark:bg-[#0B192C] text-[#0B192C] dark:text-[#D9B15B] shadow-sm'
                    : 'text-slate-600 dark:text-slate-400'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
            {filteredModules.map((mod) => {
              const enabled = isModuleEnabled(mod.alwaysEnabled, mod.businessTypes);
              const targetSpec = SCREENS_41.find((s) => s.id === mod.targetScreen);
              return (
                <button
                  key={mod.id}
                  type="button"
                  onClick={() => onJumpToScreen(mod.targetScreen)}
                  className={`p-4 rounded-xl border text-start transition-colors flex flex-col justify-between gap-2.5 ${
                    enabled
                      ? 'bg-white dark:bg-[#0F1E33] border-slate-200 dark:border-slate-800 hover:border-[#C89D42]'
                      : 'bg-slate-50/70 dark:bg-[#0B1524]/60 border-slate-200/70 dark:border-slate-800/50 opacity-65'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-mono-num text-xs font-bold text-[#9A7222] dark:text-[#D9B15B]">
                      {String(mod.number).padStart(2, '0')}
                    </span>
                    {enabled ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#15803D] dark:text-[#22C55E]">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>{isAr ? 'مفعّلة' : 'Active'}</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[11px] text-slate-400">
                        <Lock className="w-3 h-3" />
                        <span>{isAr ? 'قطاع آخر' : 'Sector Hidden'}</span>
                      </span>
                    )}
                  </div>
                  <div className="text-sm font-bold text-[#0B192C] dark:text-slate-100">
                    {mod.name[lang]}
                  </div>
                  {targetSpec && (
                    <div className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                      → {targetSpec.title[lang]}
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </section>
    </>
  );
};
