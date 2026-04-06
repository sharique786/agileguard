/**
 * LOCAL environment — default when running "ng serve"
 *
 * All backend calls are proxied to localhost:8080 via proxy.conf.json.
 * No real API tokens are needed; all services run in mock mode.
 */
export const environment = {
  name: 'local',
  production: false,
  apiUrl: '/api',                          // proxied → localhost:8080/api
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
