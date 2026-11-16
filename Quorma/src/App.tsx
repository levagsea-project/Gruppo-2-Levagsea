import Sidebar from './components/layout/Sidebar'
import HomePage from './components/home/HomePage'
import './styles/layout.css'
import './styles/sidebar.css'
import './styles/home.css'
import './styles/ui.css'

// Tutta l'app: sidebar a sinistra + pagina a destra.
export default function App() {
  return (
    <div className="app">
      <Sidebar />
      <HomePage />
    </div>
  )
}
