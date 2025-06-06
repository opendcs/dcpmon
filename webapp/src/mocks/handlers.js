import dcp from "./handlers/dcp";
import groups from "./handlers/group";
import index from "./handlers/index";
import channel from "./handlers/channel";
export const handlers = [...dcp, ...index, ...groups, ...channel];
