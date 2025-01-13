import dcp from "./handlers/dcp";
import index from "./handlers/index";
export const handlers = [...dcp, ...index];
