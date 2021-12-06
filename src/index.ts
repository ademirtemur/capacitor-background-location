import { registerPlugin } from '@capacitor/core';

import type { CapacitorBackgroundLocationPlugin } from './definitions';

const CapacitorBackgroundLocation = registerPlugin<CapacitorBackgroundLocationPlugin>(
  'CapacitorBackgroundLocation',
);

export * from './definitions';
export { CapacitorBackgroundLocation };
