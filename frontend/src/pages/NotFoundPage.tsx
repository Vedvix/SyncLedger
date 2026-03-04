import { Link } from 'react-router-dom'
import { ArrowLeft, Zap } from 'lucide-react'

export function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 px-4">
      <div className="text-center animate-fade-in max-w-md">
        {/* Logo */}
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center mx-auto mb-8 shadow-lg shadow-primary-200">
          <Zap className="w-7 h-7 text-white" />
        </div>

        {/* 404 illustration */}
        <div className="relative mb-6">
          <h1 className="text-[120px] font-extrabold leading-none bg-gradient-to-b from-gray-200 to-gray-300 bg-clip-text text-transparent select-none">
            404
          </h1>
        </div>
        
        <h2 className="text-xl font-bold text-gray-900 mb-2">Page not found</h2>
        <p className="text-gray-500 mb-8 leading-relaxed">
          The page you're looking for doesn't exist or has been moved.<br />
          Let's get you back on track.
        </p>

        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-primary-600 to-primary-500 text-white rounded-xl hover:from-primary-700 hover:to-primary-600 font-medium shadow-sm shadow-primary-200 transition-all"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Dashboard
        </Link>
      </div>
    </div>
  )
}
