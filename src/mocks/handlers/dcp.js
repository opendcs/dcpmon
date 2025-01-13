import { http, HttpResponse } from "msw";
import { LRGS_DOMAIN } from "../../constants";
import schema_dcp from "../schema/dcp.json";

const dcp = [
  // And here's a request handler with MSW
  // for the same "GET /user" request that
  // responds with a mock JSON response.
  http.get(`${LRGS_DOMAIN}/dcp`, ({ request }) => {
    return HttpResponse.json(schema_dcp);
  }),

  http.get(`${LRGS_DOMAIN}/dcp/{}`, ({ request }) => {
    return HttpResponse.json(schema_dcp);
  }),
];

export default dcp;
export { dcp };
