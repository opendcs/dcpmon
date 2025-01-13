export async function getDcpBySite(site) {
  try {
    // Dynamically import the JSON file based on the site name
    const data = await import(`../schema/dcp/${site?.toUpperCase()}.json`);
    return [data.default];
  } catch (error) {
    console.error(`Error loading data for site: ${site}`, error);
    return [];
  }
}
