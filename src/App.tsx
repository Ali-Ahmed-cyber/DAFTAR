import React, { useEffect, useState } from 'react';
import { Globe, Moon, Sun } from 'lucide-react';
import { BrandIdentitySection } from './components/BrandIdentitySection';
import { DaftarLogo } from './components/DaftarLogo';
import { DesignSystemSection } from './components/DesignSystemSection';
import { NavigationMapSection } from './components/NavigationMapSection';
import { ScreensShowcaseSection } from './components/ScreensShowcaseSection';
import { UI_LABELS } from './data/daftarSpec';
import { BusinessTypeId, Language, ScreenId, ThemeMode, UIStateMode } from './types/daftar';

export default function App() {
  const [lang, setLang] = useState<Language>('ar');
  const [theme, setTheme] = useState<ThemeMode>('light');
  const [activeScreen, setActiveScreen] = useState<ScreenId>('dashboard');
  const [historyStack, setHistoryStack] = useState<ScreenId[]>(['login']);
  const [activeBusinessType, setActiveBusinessType] = useState<BusinessTypeId>('retail');
  const [uiState, setUiState] = useState<UIStateMode>('populated');

  const isAr = lang === 'ar';

  useEffect(() => {
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
  }, [lang]);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
  }, [theme]);

  const navigateTo = (nextScreen: ScreenId) => {
    if (nextScreen === activeScreen) return;
    setHistoryStack((prev) => [...prev.slice(-12), activeScreen]);
    setActiveScreen(nextScreen);
  };

  const navigateBack = () => {
    if (historyStack.length > 0) {
      const prev = historyStack[historyStack.length - 1];
      setHistoryStack((stack) => stack.slice(0, -1));
      setActiveScreen(prev);
    } else {
      setActiveScreen(activeScreen === 'dashboard' ? 'login' : 'dashboard');
    }
  };

  const handleJumpToScreen = (screen: ScreenId) => {
    navigateTo(screen);
    const el = document.getElementById('screens-showcase');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const phases = [
    { num: '01', label: isAr ? 'الهوية والشاشات (مراجعة)' : 'Identity & 41 UIs', current: true },
    { num: '02', label: isAr ? 'قاعدة البيانات Room' : 'Room Schema', current: false },
    { num: '03', label: isAr ? 'المحاسبة المزدوجة' : 'Double-Entry', current: false },
    { num: '04', label: isAr ? 'المبيعات والمخزون' : 'Sales & Stock', current: false },
    { num: '05', label: isAr ? 'الوحدات المتخصصة' : 'Sector Modules', current: false },
    { num: '06', label: isAr ? 'التشفير والنسخ' : 'SQLCipher & Backup', current: false },
    { num: '07', label: isAr ? 'التقارير والذكاء' : 'Reports & AI', current: false },
    { num: '08', label: isAr ? 'الاختبارات الشاملة' : 'QA Suite', current: false },
    { num: '09', label: isAr ? 'تجهيز APK' : 'Android APK', current: false },
  ];

  return (
    <div className="min-h-screen bg-[#F8FAFC] dark:bg-[#08111E] text-[#0B192C] dark:text-slate-100 transition-colors">
      {/* Strict 3-Zone Top Bar Contract */}
      <header className="sticky top-0 z-40 bg-white/95 dark:bg-[#08111E]/95 backdrop-blur-md border-b border-slate-200 dark:border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-8">
          <a
            href="#top"
            className="text-xl font-extrabold tracking-tight text-[#0B192C] dark:text-white whitespace-nowrap shrink-0"
          >
            {UI_LABELS.brandTitle[lang]}
          </a>

          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-600 dark:text-slate-300">
            <a href="#brand-identity" className="hover:text-[#0B192C] dark:hover:text-[#D9B15B] transition-colors whitespace-nowrap shrink-0">
              {UI_LABELS.navBrand[lang]}
            </a>
            <a href="#design-system" className="hover:text-[#0B192C] dark:hover:text-[#D9B15B] transition-colors whitespace-nowrap shrink-0">
              {UI_LABELS.navSystem[lang]}
            </a>
            <a href="#screens-showcase" className="hover:text-[#0B192C] dark:hover:text-[#D9B15B] transition-colors whitespace-nowrap shrink-0">
              {UI_LABELS.navScreens[lang]}
            </a>
            <a href="#navigation-map" className="hover:text-[#0B192C] dark:hover:text-[#D9B15B] transition-colors whitespace-nowrap shrink-0">
              {UI_LABELS.navMap[lang]}
            </a>
            <a href="#modules-architecture" className="hover:text-[#0B192C] dark:hover:text-[#D9B15B] transition-colors whitespace-nowrap shrink-0">
              {UI_LABELS.navModules[lang]}
            </a>
          </nav>

          <div className="flex items-center shrink-0">
            <a
              href="#screens-showcase"
              className="min-h-[40px] px-4 py-2 rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-xs font-bold flex items-center justify-center whitespace-nowrap shrink-0 hover:opacity-90 transition-opacity"
            >
              {UI_LABELS.ctaExplore[lang]}
            </a>
          </div>
        </div>
      </header>

      <main id="top">
        <section className="py-12 sm:py-16">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-10">
            {/* Workspace Controls Strip */}
            <div className="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-slate-200 dark:border-slate-800">
              <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
                {UI_LABELS.phaseTag[lang]}
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setLang((l) => (l === 'ar' ? 'en' : 'ar'))}
                  className="min-h-[40px] px-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0F1E33] text-xs font-semibold flex items-center gap-2 hover:border-[#C89D42] transition-colors whitespace-nowrap"
                >
                  <Globe className="w-4 h-4 text-[#C89D42]" />
                  <span>{isAr ? 'English (LTR)' : 'العربية (RTL)'}</span>
                </button>
                <button
                  type="button"
                  onClick={() => setTheme((t) => (t === 'light' ? 'dark' : 'light'))}
                  className="min-h-[40px] px-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0F1E33] text-xs font-semibold flex items-center gap-2 hover:border-[#C89D42] transition-colors whitespace-nowrap"
                >
                  {theme === 'light' ? (
                    <>
                      <Moon className="w-4 h-4 text-[#0B192C]" />
                      <span>{isAr ? 'الوضع الليلي' : 'Dark Mode'}</span>
                    </>
                  ) : (
                    <>
                      <Sun className="w-4 h-4 text-[#D9B15B]" />
                      <span>{isAr ? 'الوضع النهاري' : 'Light Mode'}</span>
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* 2-Column Hero Split */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
              <div className="lg:col-span-7 flex flex-col justify-between gap-6">
                <div>
                  <h1 className="text-3xl sm:text-4xl lg:text-[42px] font-extrabold text-[#0B192C] dark:text-white leading-[1.2] [text-wrap:balance]">
                    {UI_LABELS.heroTitle[lang]}
                  </h1>
                  <p className="text-base text-slate-600 dark:text-slate-300 mt-4 leading-relaxed max-w-2xl">
                    {UI_LABELS.heroDesc[lang]}
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                  <button
                    type="button"
                    onClick={() => handleJumpToScreen('splash')}
                    className="min-h-[46px] px-6 py-3 rounded-xl bg-[#0B192C] dark:bg-[#C89D42] text-white dark:text-[#0B192C] text-sm font-bold whitespace-nowrap"
                  >
                    {isAr ? 'ابدأ من شاشة البداية' : 'Start from Splash Screen'}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleJumpToScreen('pharmacies')}
                    className="min-h-[46px] px-6 py-3 rounded-xl border border-[#C89D42] text-[#9A7222] dark:text-[#D9B15B] text-sm font-bold whitespace-nowrap"
                  >
                    {isAr ? 'معاينة الشاشات المتخصصة' : 'Preview Specialized Screens'}
                  </button>
                </div>
              </div>

              <div className="lg:col-span-5 bg-[#0B192C] text-white rounded-2xl p-6 sm:p-8 border border-slate-800 flex flex-col justify-between gap-6">
                <div className="flex items-center gap-3">
                  <DaftarLogo variant="gold-on-navy" size={52} />
                  <div>
                    <div className="text-lg font-bold">{UI_LABELS.brandFull[lang]}</div>
                    <div className="text-xs text-slate-300">
                      {isAr ? 'خطة التنفيذ المعتمدة (9 مراحل)' : 'Approved 9-Phase Execution Plan'}
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-2">
                  {phases.map((p) => (
                    <div
                      key={p.num}
                      className={`p-2.5 rounded-xl border text-start ${
                        p.current
                          ? 'bg-[#C89D42] text-[#0B192C] border-[#C89D42]'
                          : 'bg-[#132642] text-slate-300 border-white/10'
                      }`}
                    >
                      <div className="font-mono-num text-[10px] font-bold">{p.num}</div>
                      <div className="text-[11px] font-bold mt-0.5 truncate">{p.label}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* 4-Column Architecture Metrics */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              {[
                { val: '41', label: isAr ? 'شاشة أساسية ومتخصصة' : 'Core & Specialized Screens' },
                { val: '37', label: isAr ? 'وحدة مغطاة بالكامل' : 'Fully Covered Modules' },
                { val: '5', label: isAr ? 'حالات واجهة لكل شاشة' : 'UI States per Screen' },
                { val: '6', label: isAr ? 'أنشطة بواجهات معزولة' : 'Isolated Business Vaults' },
              ].map((item) => (
                <div
                  key={item.label}
                  className="bg-white dark:bg-[#0F1E33] rounded-xl border border-slate-200 dark:border-slate-800 p-5"
                >
                  <div className="font-mono-num text-2xl sm:text-3xl font-bold text-[#0B192C] dark:text-[#D9B15B]">
                    {item.val}
                  </div>
                  <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">{item.label}</div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <BrandIdentitySection lang={lang} />
        <DesignSystemSection lang={lang} theme={theme} />

        <ScreensShowcaseSection
          lang={lang}
          theme={theme}
          activeScreen={activeScreen}
          activeBusinessType={activeBusinessType}
          uiState={uiState}
          onChangeUiState={setUiState}
          onNavigate={navigateTo}
          onBack={navigateBack}
          onSelectBusinessType={setActiveBusinessType}
          onToggleLang={() => setLang((l) => (l === 'ar' ? 'en' : 'ar'))}
          onToggleTheme={() => setTheme((t) => (t === 'light' ? 'dark' : 'light'))}
        />

        <NavigationMapSection
          lang={lang}
          activeScreen={activeScreen}
          activeBusinessType={activeBusinessType}
          onJumpToScreen={handleJumpToScreen}
          onSelectBusinessType={setActiveBusinessType}
        />
      </main>

      <footer className="border-t border-slate-200 dark:border-slate-800 py-8 bg-white dark:bg-[#08111E]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500 dark:text-slate-400">
          <div>
            {isAr
              ? 'دفتر — المحاسب الذكي · المرحلة الأولى المكتملة: 41 شاشة · 37 وحدة · 5 حالات واجهة'
              : 'DAFTAR — Smart Accounting · Completed Phase 1: 41 Screens · 37 Modules · 5 UI States'}
          </div>
          <div>
            {isAr
              ? 'تصميم واجهات وخريطة تنقل فقط — لم تبدأ المرحلة الثانية (قاعدة البيانات) بعد'
              : 'UI Design & Navigation Map Only — Phase 2 (Database) Pending Approval'}
          </div>
        </div>
      </footer>
    </div>
  );
}
