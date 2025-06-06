import { useState } from "react";
// import reactLogo from './assets/react.svg'
// import bootstrap css
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import ReportSelect from "./components/ReportSelect";
import Report from "./components/Report";
import SiteWrapper from "./components/composite/SiteWrapper.jsx";
import { Route, Routes } from "react-router-dom";
const queryClient = new QueryClient();

function App() {
  const [form, setForm] = useState(null);

  return (
    <QueryClientProvider client={queryClient}>
      <SiteWrapper>
        <Routes>
          <Route
            path="/"
            element={
              <div className="p-5">
                <h2>DCPMon</h2>
                {!form ? <ReportSelect onForm={setForm} /> : <Report />}
              </div>
            }
          />
          {/* <Route path="/about" element={<About />} />
        <Route path="*" element={<NotFound />} /> */}
        </Routes>
      </SiteWrapper>
    </QueryClientProvider>
  );
}

export default App;
