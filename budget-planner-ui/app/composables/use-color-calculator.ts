export function useColorCalculator() {
  /**
   * Approximation of nuxt-ui primary color.
   */
  const baseHue = 217;
  const saturation = 91;
  const lightness = 60;

  /**
   * Generates a list of colors by shifting the hue of the primary color.
   * Based on HSL, shifting hue by 360/32 degrees each time.
   */
  function generateCategoryColors(count: number = 32): string[] {
    const colors: string[] = [];
    const hueShift = 360 / count;

    for (let i = 0; i < count; i++) {
      const hue = (baseHue + (i * hueShift)) % 360;
      colors.push(`hsl(${hue}, ${saturation}%, ${lightness}%)`);
    }

    return colors;
  }

  function getCategoryColor(index: number, count: number = 32): string {
    const colors = generateCategoryColors(count);
    return colors[index % count] ?? `hsl(${baseHue}, ${saturation}%, ${lightness}%)`;
  }

  return {
    getCategoryColor
  };
}
