/**
 * UAT environment — User Acceptance Testing.
 *
 * Build command : ng build --configuration=uat
 * Serve command : ng serve --configuration=uat
 *
 * Connected to the UAT backend with real JIRA, Gemini, and GitHub data.
 * Debug logging is off to match production behaviour.
 */
export const environment = {
  name: 'uat',
  production: false,                        // not minified, source maps on
  apiUrl: 'https://uat-api.agileguard.example.com/api',
  jiraProjectKey: 'COMMSSURV',
  defaultPageSize: 20,
  enableDebugLogging: false,
  featureFlags: {
    enableLeaderboard: true,
    enableAiValidation: true,
    enableCiCdMonitoring: true,
    enableExport: true,
  }
};
