package com.example.bookinghelper.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.bookinghelper.R;
import com.example.bookinghelper.util.TemplateManager;

public class SettingsFragment extends Fragment {

    private EditText etContactFormat, etTemplateBody;
    private TextView tvContactPreviewExample;
    private AppCompatButton btnSaveSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etContactFormat = view.findViewById(R.id.etSettingContactFormat);
        etTemplateBody = view.findViewById(R.id.etSettingTemplateBody);
        tvContactPreviewExample = view.findViewById(R.id.tvContactPreviewExample);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);

        // 현재 설정값 로드
        etContactFormat.setText(TemplateManager.getContactFormat(requireContext()));
        etTemplateBody.setText(TemplateManager.getTemplate(requireContext()));
        updateExamplePreview(etContactFormat.getText().toString());

        // 연락처 칩 클릭 시 커서 위치에 변수 삽입
        view.findViewById(R.id.chipContactName).setOnClickListener(v -> insertVariable(etContactFormat, "{이름}"));
        view.findViewById(R.id.chipContactSubject).setOnClickListener(v -> insertVariable(etContactFormat, "{과목}"));
        view.findViewById(R.id.chipContactBranch).setOnClickListener(v -> insertVariable(etContactFormat, "{지점}"));

        // 템플릿 칩 클릭 시 커서 위치에 변수 삽입
        view.findViewById(R.id.chipTemplateName).setOnClickListener(v -> insertVariable(etTemplateBody, "{이름}"));
        view.findViewById(R.id.chipTemplateSubject).setOnClickListener(v -> insertVariable(etTemplateBody, "{과목}"));
        view.findViewById(R.id.chipTemplateDate).setOnClickListener(v -> insertVariable(etTemplateBody, "{일시}"));

        // 연락처 형식 개별 초기화 (에디터만 변경, 저장 버튼으로 확정)
        view.findViewById(R.id.btnResetContact).setOnClickListener(v -> {
            etContactFormat.setText(TemplateManager.DEFAULT_CONTACT_FORMAT);
            updateExamplePreview(TemplateManager.DEFAULT_CONTACT_FORMAT);
            Toast.makeText(requireContext(), "연락처 형식이 기본값으로 입력되었습니다. 저장 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show();
        });

        // 문자 템플릿 개별 초기화 (에디터만 변경, 저장 버튼으로 확정)
        view.findViewById(R.id.btnResetTemplate).setOnClickListener(v -> {
            etTemplateBody.setText(TemplateManager.DEFAULT_TEMPLATE);
            Toast.makeText(requireContext(), "문자 템플릿이 기본값으로 입력되었습니다. 저장 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show();
        });

        // 텍스트 변경 시 저장 예시만 실시간 업데이트 (자동 저장 X)
        etContactFormat.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateExamplePreview(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // [설정 저장하기] 버튼 클릭 시 확정 저장
        btnSaveSettings.setOnClickListener(v -> {
            String contactFormat = etContactFormat.getText().toString().trim();
            String templateBody = etTemplateBody.getText().toString().trim();

            if (contactFormat.isEmpty()) {
                Toast.makeText(requireContext(), "연락처 저장 형식을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (templateBody.isEmpty()) {
                Toast.makeText(requireContext(), "문자 템플릿 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            TemplateManager.saveContactFormat(requireContext(), contactFormat);
            TemplateManager.saveTemplate(requireContext(), templateBody);

            // 대기열 예약과의 혼선을 방지하기 위한 안내 문구 반영
            Toast.makeText(requireContext(), "설정이 저장되었습니다.\n(이후 접수되는 새 예약부터 적용됩니다)", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateExamplePreview(String format) {
        String example = format.replace("{이름}", "김민지")
                .replace("{과목}", "기타")
                .replace("{지점}", "여의도점");
        tvContactPreviewExample.setText(example);
    }

    private void insertVariable(EditText editText, String variable) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editText.getText().replace(Math.min(start, end), Math.max(start, end), variable, 0, variable.length());
        editText.requestFocus();
    }
}