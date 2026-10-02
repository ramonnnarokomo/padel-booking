/** Name and email remembered after a successful booking to pre-fill the forms. */
export interface Player {
  name: string;
  email: string;
}

const STORAGE_KEY = 'padelbook.player';

// localStorage can throw (private mode, blocked cookies...), so failures are ignored.

export function loadPlayer(): Player | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Player) : null;
  } catch {
    return null;
  }
}

export function savePlayer(player: Player): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(player));
  } catch {
    // Nothing to do: the forms will simply start empty next time.
  }
}
