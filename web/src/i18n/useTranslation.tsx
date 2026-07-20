import { useState, useCallback, createContext, useContext, type ReactNode } from 'react'
import zh from './zh-CN'
import en from './en-US'

type Locale = 'zh-CN' | 'en-US'
const translations = { 'zh-CN': zh, 'en-US': en }

interface TranslationContextType {
  locale: Locale
  setLocale: (l: Locale) => void
  t: (key: string) => string
}

const TranslationContext = createContext<TranslationContextType>({
  locale: 'zh-CN',
  setLocale: () => {},
  t: (k: string) => k,
})

export function TranslationProvider({ children }: { children: ReactNode }) {
  const [locale, setLocale] = useState<Locale>('zh-CN')
  const t = useCallback((key: string) => translations[locale][key] || key, [locale])
  return <TranslationContext.Provider value={{ locale, setLocale, t }}>{children}</TranslationContext.Provider>
}

export function useTranslation() {
  return useContext(TranslationContext)
}
