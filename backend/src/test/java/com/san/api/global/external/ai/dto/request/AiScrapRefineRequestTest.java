package com.san.api.global.external.ai.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiScrapRefineRequestTest {

    @Test
    void toTilRequest_convertsToSingleContentTilGenerationRequest() {
        AiScrapRefineRequest request = new AiScrapRefineRequest("TEXT", "raw content");

        AiTilRequest tilRequest = request.toTilRequest();

        assertThat(tilRequest.generateTil()).isTrue();
        assertThat(tilRequest.contents()).hasSize(1);
        assertThat(tilRequest.contents().get(0).inputType()).isEqualTo("TEXT");
        assertThat(tilRequest.contents().get(0).content()).isEqualTo("raw content");
    }
}
