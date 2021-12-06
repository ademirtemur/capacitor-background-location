import { registerPlugin } from '@capacitor/core';

import type { CapacitorBackgroundLocationPlugin } from './definitions';

const CapacitorBackgroundLocation = registerPlugin<CapacitorBackgroundLocationPlugin>(
  'CapacitorBackgroundLocation',
  {
    web: () =>
      import('./web').then(m => new m.CapacitorBackgroundLocationWeb()),
  },
);

export * from './definitions';
export { CapacitorBackgroundLocation };
