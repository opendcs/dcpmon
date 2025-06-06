import { http, HttpResponse, passthrough } from "msw";
import { LRGS_DOMAIN } from "../../constants";
import schema_dcp from "../schema/dcp";
import { getDcpByAddress } from "../utils/dcp";

const dcp = [
  // And here's a request handler with MSW
  // for the same "GET /user" request that
  // responds with a mock JSON response.
  http.get(`${LRGS_DOMAIN}/dcp`, ({ request, params, cookies }) => {
    return HttpResponse.json(schema_dcp);
  }),

  http.get(
    `${LRGS_DOMAIN}/dcp/:address`,
    async ({ request, params, cookies }) => {
      const { address } = params;
      const addressData = address ? await getDcpByAddress(address) : schema_dcp;
      if (address.error) {
        return HttpResponse.json({ message: address.error }, { status: 404 });
      }

      console.log({ address });

      return HttpResponse.json(address);
    }
  ),

  // Although this handler also matches the request,
  // it will never be called because the previous handler
  // returned a mocked response.
  http.get("/user", () => passthrough()),
];

export default dcp;
export { dcp };
