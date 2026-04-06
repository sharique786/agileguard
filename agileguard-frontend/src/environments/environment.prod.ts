/**
 * PRODUCTION environment.
 *
 * Build command : ng build --configuration=production
 *
 * - production: true  → Angular enables full tree-shaking, AOT, minification.
 * - enableDebugLogging: false → console.debug calls are stripped by the compiler.
 * - apiUrl points to the production gateway domain.
 */
export const environment = {
  name: 'prod',
  production: true,
  apiUrl: 'https://api.agileguard.example.com/api',
  jiraProjectKey: 'COMMSSURV',
  defaultPageSize: 25,
  enableDebugLogging: false,
  featureFlags: {
    enableLeaderboard: true,
    enableAiValidation: true,
    enableCiCdMonitoring: true,
    enableExport: true,
  }
};
