export const useSidebar = () => {
  const open = useState('sidebar-open', () => true)

  const toggle = () => {
    open.value = !open.value
  }

  return {
    open,
    toggle
  }
}
