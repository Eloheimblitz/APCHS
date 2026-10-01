function base(props) {
  return { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', ...props };
}

export function HouseholdIcon(props) {
  return (
    <svg {...base(props)}>
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5.5 10v9.5h13V10" />
      <path d="M10 19.5v-6h4v6" />
    </svg>
  );
}

export function MapPinIcon(props) {
  return (
    <svg {...base(props)}>
      <path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21Z" />
      <circle cx="12" cy="9.5" r="2.4" />
    </svg>
  );
}

export function BarChartIcon(props) {
  return (
    <svg {...base(props)}>
      <path d="M4 20V10" />
      <path d="M12 20V4" />
      <path d="M20 20v-7" />
    </svg>
  );
}

export function UsersIcon(props) {
  return (
    <svg {...base(props)}>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" />
      <path d="M15.5 5.2a3.2 3.2 0 0 1 0 6.1" />
      <path d="M17.5 14.3c2.6.5 4.5 2.8 4.5 5.7" />
    </svg>
  );
}

export function CalendarIcon(props) {
  return (
    <svg {...base(props)}>
      <rect x="3.5" y="5" width="17" height="15.5" rx="2.2" />
      <path d="M3.5 10h17" />
      <path d="M8 3v4" />
      <path d="M16 3v4" />
    </svg>
  );
}

export function SyringeIcon(props) {
  return (
    <svg {...base(props)}>
      <path d="m18 3 3 3" />
      <path d="M17 4 6.5 14.5l-3 6 6-3L20 7" />
      <path d="m9 11 4 4" />
      <path d="m12 8 4 4" />
    </svg>
  );
}

export function HeartPulseIcon(props) {
  return (
    <svg {...base(props)}>
      <path d="M12 20.5s-7.5-4.6-9.8-9.4C.6 7.6 2.4 4 6 4c2 0 3.4 1.1 4.2 2.3L12 9l.9-1.2" />
      <path d="M22 11h-3l-1.6 3-2.4-6-1.6 3H10" />
    </svg>
  );
}

export function ActivityIcon(props) {
  return (
    <svg {...base(props)}>
      <path d="M3 12h4l2.2-7 4 14 2-9.5 1.3 2.5H21" />
    </svg>
  );
}
