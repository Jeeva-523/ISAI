import { config } from '@kolhe/eslint-config'

export default config(
  [
    {
      ignores: ['test_resolve.cjs', '*.cjs', 'dist/**', 'android-app/**', 'scratch/**']
    },
    {
      rules: {
        'prettier/prettier': ['warn', { endOfLine: 'auto' }],
        'unicorn/filename-case': [
          'error',
          {
            cases: {
              kebabCase: true,
              pascalCase: true,
              camelCase: true
            }
          }
        ]
      }
    },
    {
      files: ['src/**/*.ts'],
      rules: {
        'import/no-default-export': 'off'
      }
    }
  ],
  {
    prettier: true,
    markdown: true
  }
)
