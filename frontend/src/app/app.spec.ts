import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { Toaster } from './core/toaster';

describe('App', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    });
  });

  it('renders the header with the navigation links', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.brand')?.textContent).toContain('PadelBook');
    const links = [...element.querySelectorAll('nav a')].map((link) => link.textContent?.trim());
    expect(links).toEqual(['Reservar', 'Mis reservas']);
  });

  it('announces toast messages in a live region', async () => {
    const fixture = TestBed.createComponent(App);
    TestBed.inject(Toaster).show('Reserva confirmada');
    await fixture.whenStable();

    const region = (fixture.nativeElement as HTMLElement).querySelector('[aria-live]');
    expect(region?.getAttribute('aria-live')).toBe('polite');
    expect(region?.textContent).toContain('Reserva confirmada');
  });
});
