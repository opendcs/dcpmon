import { dcpHandlers } from "./handlers/dcp";
import { sourceHandlers } from "./handlers/sources";
import { queryHandlers } from "./handlers/query";

export const handlers = [
  ...dcpHandlers,
  ...sourceHandlers,
  ...queryHandlers
];

