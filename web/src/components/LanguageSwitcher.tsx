import { useTranslation } from '../i18n/useTranslation'
import { Globe } from 'lucide-react'

export function LanguageSwitcher() {
  const { locale, setLocale } = useTranslation()
  return (
    <button
      onClick={() => setLocale(locale === 'zh-CN' ? 'en-US' : 'zh-CN')}
      className="flex items-center gap-1.5 px-2 py-1 rounded-lg text-gray-500 hover:text-gray-300 hover:bg-white/[0.04] transition-colors text-xs"
    >
      <Globe className="w-3.5 h-3.5" />
      {locale === 'zh-CN' ? 'EN' : '中文'}
    </button>
  )
}
