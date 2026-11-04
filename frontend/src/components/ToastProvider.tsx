import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react'
import { ToastContext } from '../lib/toast.ts'

export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<string | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined)

  const show = useCallback((text: string) => {
    setMessage(text)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setMessage(null), 2600)
  }, [])

  useEffect(() => () => clearTimeout(timer.current), [])

  return (
    <ToastContext value={show}>
      {children}
      {/* role="status" : toujours présent dans la page, pour que chaque nouveau message soit annoncé */}
      <div
        role="status"
        aria-live="polite"
        className={`pointer-events-none fixed bottom-6 left-1/2 z-[60] flex -translate-x-1/2 items-center gap-2 rounded-full bg-stone-900 px-5 py-3 text-sm text-white transition duration-200 before:size-2 before:rounded-full before:bg-safran ${
          message ? 'translate-y-0 opacity-100' : 'translate-y-5 opacity-0'
        }`}
      >
        {message}
      </div>
    </ToastContext>
  )
}
