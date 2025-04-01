import { http, HttpResponse, passthrough } from "msw";
import { LRGS_DOMAIN } from "../../constants";
import schema_dcp from "../schema/dcp";
import { getDcpBySite } from "../utils/dcp";

const dcp = [
  // And here's a request handler with MSW
  // for the same "GET /user" request that
  // responds with a mock JSON response.
  http.get(`${LRGS_DOMAIN}/dcp`, ({ request, params, cookies }) => {
    return HttpResponse.json(schema_dcp);
  }),

  http.get(`${LRGS_DOMAIN}/dcp/:site`, async ({ request, params, cookies }) => {
    const { site } = params;
    const siteData = site ? await getDcpBySite(site) : schema_dcp;
    if (siteData.error) {
      return HttpResponse.json({ message: siteData.error }, { status: 404 });
    }

    console.log({ siteData });

    return HttpResponse.json(siteData);
  }),

  // Although this handler also matches the request,
  // it will never be called because the previous handler
  // returned a mocked response.
  http.get("/user", () => passthrough()),
];

export default dcp;
export { dcp };
