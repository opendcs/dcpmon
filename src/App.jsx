import { useState } from "react";
// import reactLogo from './assets/react.svg'
// import bootstrap css
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import ReportSelect from "./components/ReportSelect";
import Report from "./components/Report";

const queryClient = new QueryClient();

function App() {
  const [form, setForm] = useState(null);

  return (
    <QueryClientProvider client={queryClient}>
      <div className="p-5">
        <h2>DCPMon</h2>
        {!form ? <ReportSelect onForm={setForm} /> : <Report />}
      </div>
    </QueryClientProvider>
  );
}

export default App;
