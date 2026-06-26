import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          50: '#f5f5f5',
          100: '#e0e0e0',
          200: '#b3b3b3',
          300: '#808080',
          400: '#4d4d4d',
          500: '#333333',
          600: '#2a2a2a',
          700: '#1a1a1a',
          800: '#0d0d0d',
          900: '#050505',
        },
        accent: {
          DEFAULT: '#FFD700',
          50: '#FFF8E0',
          100: '#FFF0B3',
          200: '#FFE880',
          300: '#FFE04D',
          400: '#FFD81A',
          500: '#FFD700',
          600: '#CCAC00',
          700: '#998100',
          800: '#665600',
          900: '#332B00',
        },
      },
    },
  },
  plugins: [],
};

export default config;
