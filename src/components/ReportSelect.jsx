import InputRadio from "./Forms/InputRadio";
import Container from "react-bootstrap/Container";
import Row from "react-bootstrap/Row";
import Col from "react-bootstrap/Col";
import Dropdown from "react-bootstrap/Dropdown";
import DropdownButton from "react-bootstrap/DropdownButton";
import Form from "react-bootstrap/Form";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { LRGS_DOMAIN } from "../constants";

export default function ReportSelect({ setForm }) {
  const [groups, setGroups] = useState([]);

  const {
    data: groupData,
    error: groupError,
    isPending: groupPending,
  } = useQuery({
    queryKey: ["groups"],
    queryFn: async () => {
      return fetch(`${LRGS_DOMAIN}/groups`).then((res) => res.json());
    },
  });
  const {
    data: channelData,
    error: channelError,
    isPending: channelPending,
  } = useQuery({
    queryKey: ["channel"],
    queryFn: async () => {
      return fetch(`${LRGS_DOMAIN}/channel`).then((res) => res.json());
    },
    select: (data) => {
      return data?.channels;
    },
  });
  console.log({ channelData });
  return (
    <Container>
      <h1>Report Select</h1>
      <Row className="mb-3">
        <Col sm={2}>
          <InputRadio
            onChange={setForm}
            group="report_text"
            text="DCP Group"
            formId="group_select"
          />
        </Col>
        <Col>
          <DropdownButton
            id="dropdown-group-select"
            title="Group Select"
            defaultValue={"Select Group"}
          >
            {groupData?.map((group) => (
              <Dropdown.Item key={group.id} href={`#${group.id}`}>
                {group.id}
              </Dropdown.Item>
            ))}
          </DropdownButton>
        </Col>
      </Row>
      <Row className="mb-3">
        <Col sm={2}>
          <InputRadio
            onChange={setForm}
            group="report_text"
            text="Channel"
            formId="channel_select"
          />
        </Col>
        <Col>
          <DropdownButton id="dropdown-channel-select" title="Dropdown button">
            {channelData?.map((channel, idx) => (
              <Dropdown.Item key={channel + "_" + idx} href={`#${channel}`}>
                {channel}
              </Dropdown.Item>
            ))}
          </DropdownButton>
        </Col>
      </Row>
      <Row className="mb-3">
        <Col sm={2}>
          <InputRadio
            onChange={setForm}
            group="report_text"
            text="Enter DCP name/address"
            formId="dcp_text"
          />
        </Col>
        <Col>
          <Form.Control
            placeholder="CE609B50"
            aria-label="DCP Name/Address"
            aria-describedby="basic-addon1"
          />
        </Col>
      </Row>
    </Container>
  );
}
