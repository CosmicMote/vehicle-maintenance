import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'vm-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly htmlEl = document.documentElement;

  isDark = signal(false);

  constructor() {
    const saved = localStorage.getItem(STORAGE_KEY);
    const prefersDark = saved !== null
      ? saved === 'dark'
      : window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.apply(prefersDark);
  }

  toggle() {
    this.apply(!this.isDark());
  }

  private apply(dark: boolean) {
    this.isDark.set(dark);
    this.htmlEl.classList.toggle('dark', dark);
    localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light');
  }
}
