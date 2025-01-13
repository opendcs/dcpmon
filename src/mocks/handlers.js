import { http, HttpResponse } from "msw";
import { LRGS_DOMAIN } from "../constants";
import schema from "./schema.json";

export const handlers = [
  // And here's a request handler with MSW
  // for the same "GET /user" request that
  // responds with a mock JSON response.
  http.get(`${LRGS_DOMAIN}/user`, ({ request }) => {
    console.log(schema.users);
    return HttpResponse.json(schema.users);
  }),
];
