import { PluginListenerHandle } from "@capacitor/core";

export enum LOCATION_PRIORITY_ANDROID {
  PRIORITY_HIGH_ACCURACY = 100,
  PRIORITY_BALANCED_POWER_ACCURACY = 102,
  PRIORITY_LOW_POWER = 104,
  PRIORITY_NO_POWER = 105,
}

export enum EVENTS {
  Change = "CHANGE",
  Error = "ERROR"
}

export interface IStartOptions {
  interval?: number;
  locationPriority?: LOCATION_PRIORITY_ANDROID;
}

export interface ILocation {
  latitude?: number;
  longitude?: number;
  accuracy?: number;
  altitude?: number;
  bearing?: number;
  angle?: number;
  speed?: number;
  time?: number | string;
}

export interface IConfig {
  title: string;
  description: string;
  url?: string;
  headers?: { [key: string]: string };
  body?: { [key: string]: any };
}

export interface CapacitorBackgroundLocationPlugin {
  setConfig(config: IConfig): Promise<{}>;
  start(options: IStartOptions): Promise<{}>;
  stop(): Promise<{}>;
  addListener(eventName: EVENTS.Change, callback: (location: ILocation) => void): Promise<PluginListenerHandle> & PluginListenerHandle;
  addListener(eventName: EVENTS.Error, callback: (err?: any) => void): Promise<PluginListenerHandle> & PluginListenerHandle;
  removeAllListeners(): Promise<void>;
}
