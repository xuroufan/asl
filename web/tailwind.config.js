/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#4F8CF7', light: '#7AACFF', dark: '#3A6FD4' },
        buy: '#FF6B6B',
        sell: '#00C853',
        flat: '#9E9E9E',
        surface: { DEFAULT: '#121A2E', light: '#1A2744', dark: '#0B0F1A' },
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
      },
    },
  },
  plugins: [],
}
