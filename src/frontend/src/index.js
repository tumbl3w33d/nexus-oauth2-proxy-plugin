import { createRoot } from 'react-dom/client';
import OAuth2ProxyApiTokenComponent from './components/OAuth2ProxyApiTokenComponent';

const container = document.getElementById('app');
const root = createRoot(container);
root.render(<OAuth2ProxyApiTokenComponent />);