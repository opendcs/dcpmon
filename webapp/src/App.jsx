import { useEffect } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import SiteWrapper from "./components/composite/SiteWrapper.jsx";
import { Route, Routes } from "react-router-dom";
import { DefaultApi } from "dds-api";
import useDataQuery from "./hooks/useDataQuery.js";
import useGroupSummary from "./hooks/useGroupSummary.js";

// Start MSW conditionally in dev
if (import.meta.env.DEV) {
  const setupMocks = async () => {
    const { worker } = await import("./mocks/browser");
    await worker.start({
      onUnhandledRequest: "bypass",
    });
  };
  setupMocks();
}

const queryClient = new QueryClient();

function App() {

  const goesDcp = useDataQuery({dataParams: {source: "goes"}})
  console.log(goesDcp.data)
  const swtGroup = useGroupSummary({dataParams: {group: "swt"}})
    console.log(swtGroup.data)
  return (
    <QueryClientProvider client={queryClient}>
      <SiteWrapper>
        <Routes>
          <Route
            path="/"
            element={
              <div className="p-5">
                <h2>DCPMon</h2>
                <div>{JSON.stringify(goesDcp?.data)}</div>
              {/*  {!form ? <ReportSelect onForm={setForm} /> : <Report />} */}
              </div>
            }
          />
        </Routes>
      </SiteWrapper>
    </QueryClientProvider>
  );
}

export default App;
