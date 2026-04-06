/**
 * DEV environment — used when building for the development server.
 *
 * Build command : ng build --configuration=development
 * Serve command : ng serve --configuration=development
 *
 * API calls go directly to the deployed dev gateway.
 * Replace the apiUrl with your actual dev server URL.
 */
export const environment = {
  name: 'dev',
  production: false,
  apiUrl: 'https://dev-api.agileguard.example.com/api',
  jiraProjectKey: 'COMMSSURV',
  defaultPageSize: 20,
  enableDebugLogging: true,
  featureFlags: {
    enableLeaderboard: true,
    enableAiValidation: true,
    enableCiCdMonitoring: true,
    enableExport: true,
  }
};
