import { WebPlugin } from '@capacitor/core';

import type { CapacitorBackgroundLocationPlugin } from './definitions';

export class CapacitorBackgroundLocationWeb
  extends WebPlugin
  implements CapacitorBackgroundLocationPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
