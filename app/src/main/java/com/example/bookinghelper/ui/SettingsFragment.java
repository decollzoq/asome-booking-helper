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
import androidx.fragment.app.Fragment;

import com.example.bookinghelper.R;
import com.example.bookinghelper.util.TemplateManager;

public class SettingsFragment extends Fragment {

    private EditText etContactFormat, etTemplateBody;
    private TextView tvContactPreviewExample;

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

        // 현재 저장된 설정값 로드
        etContactFormat.setText(TemplateManager.getContactFormat(requireContext()));
        etTemplateBody.setText(TemplateManager.getTemplate(requireContext()));
        updateExamplePreview(etContactFormat.getText().toString());

        // 연락처 변수 칩 클릭 시 커서 위치에 변수 삽입
        view.findViewById(R.id.chipContactName).setOnClickListener(v -> insertVariable(etContactFormat, "{이름}"));
        view.findViewById(R.id.chipContactSubject).setOnClickListener(v -> insertVariable(etContactFormat, "{과목}"));
        view.findViewById(R.id.chipContactBranch).setOnClickListener(v -> insertVariable(etContactFormat, "{지점}"));

        // 템플릿 변수 칩 클릭 시 커서 위치에 변수 삽입
        view.findViewById(R.id.chipTemplateName).setOnClickListener(v -> insertVariable(etTemplateBody, "{이름}"));
        view.findViewById(R.id.chipTemplateSubject).setOnClickListener(v -> insertVariable(etTemplateBody, "{과목}"));
        view.findViewById(R.id.chipTemplateDate).setOnClickListener(v -> insertVariable(etTemplateBody, "{일시}"));

        // 1. 연락처 저장 형식 액션 버튼 3종 (초기화 / 기본값 / 저장)
        view.findViewById(R.id.btnClearContact).setOnClickListener(v -> {
            etContactFormat.setText("");
            etContactFormat.requestFocus();
        });

        view.findViewById(R.id.btnDefaultContact).setOnClickListener(v -> {
            etContactFormat.setText(TemplateManager.DEFAULT_CONTACT_FORMAT);
            updateExamplePreview(TemplateManager.DEFAULT_CONTACT_FORMAT);
            Toast.makeText(requireContext(), "기본 연락처 형식이 입력되었습니다.\n(저장 버튼을 눌러야 저장됩니다.)", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnSaveContact).setOnClickListener(v -> {
            String contactFormat = etContactFormat.getText().toString().trim();
            if (contactFormat.isEmpty()) {
                Toast.makeText(requireContext(), "연락처 저장 형식을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            TemplateManager.saveContactFormat(requireContext(), contactFormat);
            Toast.makeText(requireContext(), "연락처 저장 형식이 저장되었습니다.\n(이후 접수되는 새 예약부터 적용됩니다)", Toast.LENGTH_SHORT).show();
        });

        // 2. 문자 템플릿 액션 버튼 3종 (초기화 / 기본값 / 저장)
        view.findViewById(R.id.btnClearTemplate).setOnClickListener(v -> {
            etTemplateBody.setText("");
            etTemplateBody.requestFocus();
        });

        view.findViewById(R.id.btnDefaultTemplate).setOnClickListener(v -> {
            etTemplateBody.setText(TemplateManager.DEFAULT_TEMPLATE);
            Toast.makeText(requireContext(), "기본 문자 템플릿이 입력되었습니다.\n(저장 버튼을 눌러야 저장됩니다.)", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnSaveTemplate).setOnClickListener(v -> {
            String templateBody = etTemplateBody.getText().toString().trim();
            if (templateBody.isEmpty()) {
                Toast.makeText(requireContext(), "문자 템플릿 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            TemplateManager.saveTemplate(requireContext(), templateBody);
            Toast.makeText(requireContext(), "문자 템플릿이 저장되었습니다.\n(이후 접수되는 새 예약부터 적용됩니다)", Toast.LENGTH_SHORT).show();
        });

        // 연락처 형식 텍스트 변경 시 실시간 예시 업데이트
        etContactFormat.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateExamplePreview(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateExamplePreview(String format) {
        if (format.trim().isEmpty()) {
            tvContactPreviewExample.setText("-");
            return;
        }
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