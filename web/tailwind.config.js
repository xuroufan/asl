/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#4F8CF7', light: '#7AACFF', dark: '#3A6FD4', subtle: 'rgba(79,140,247,0.08)' },
        buy: '#FF6B6B',
        sell: '#00C853',
        flat: '#9E9E9E',
        surface: { DEFAULT: '#121A2E', light: '#1A2744', dark: '#0B0F1E', raised: '#0F1729' },
        ink: { DEFAULT: '#080C1A', lighter: '#0D1322', light: '#1A2744' },
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'grid-scroll': 'gridScroll 20s linear infinite',
        'pulse-slow': 'pulse 4s ease-in-out infinite',
        'price-flash': 'priceFlash 0.6s ease-out',
        'shimmer': 'shimmer 2s ease-in-out infinite',
        'breathe': 'breathe 3s ease-in-out infinite',
        'scale-in': 'scaleIn 0.2s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        gridScroll: {
          '0%': { transform: 'translateY(0)' },
          '100%': { transform: 'translateY(-50%)' },
        },
        priceFlash: {
          '0%': { opacity: '1' },
          '50%': { opacity: '0.6' },
          '100%': { opacity: '1' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        breathe: {
          '0%, 100%': { opacity: '0.4' },
          '50%': { opacity: '0.8' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
      backgroundImage: {
        'shimmer': 'linear-gradient(90deg, transparent 0%, rgba(79,140,247,0.04) 50%, transparent 100%)',
        'glow-radial': 'radial-gradient(ellipse at 50% 0%, rgba(79,140,247,0.06) 0%, transparent 70%)',
      },
    },
  },
  plugins: [],
}
