export interface CapacitorBackgroundLocationPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
