import { Navigate, useLocation } from 'react-router-dom';

export function LegacyResultRedirect() {
  const location = useLocation();
  return <Navigate to={`/archive${location.search}`} replace />;
}
