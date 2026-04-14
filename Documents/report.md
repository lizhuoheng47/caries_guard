## 涓€銆佹ā鍧楀畾浣?

`caries-report` 鐨勮亴璐ｅ凡缁忓喕缁撳緱寰堟竻妤氾細

- 鎶ュ憡妯℃澘绠＄悊
- 鍖荤敓鐗堟姤鍛婄敓鎴?
- 鎮ｈ€呯増鎶ュ憡鐢熸垚
- PDF 鐢熸垚銆佺増鏈綊妗?
- 鎶ュ憡瀵煎嚭瀹¤

鏍稿績瀵硅薄灏辨槸锛?

- `rpt_template`
- `rpt_record`
- `rpt_export_log`銆?

P5 鐨勯獙鏀舵爣鍑嗕篃宸茬粡鍥哄畾锛?

- 鍚屼竴鐥呬緥鍙敓鎴愬尰鐢熺増涓庢偅鑰呯増鎶ュ憡
- 鎶ュ憡鐗堟湰涓嶅彲瑕嗙洊
- 瀵煎嚭鍙璁?
- 鎶ュ憡鍒楄〃涓庤鎯呭彲鏌ャ€?

------

## 浜屻€佸惎鍔ㄥ墠 Gate

report 涓嶆槸浠庨浂璧锋锛屽畠渚濊禆 analysis 鐨勪骇鐗┿€備綘鐜板湪 analysis 宸茬粡鍏峰锛?

- AI 鎽樿
- visual assets
- risk assessment
- 鍖荤敓淇鍏ュ彛
- 鐥呬緥鐘舵€佹祦杞粺涓€璧?case 妯″潡

杩欐剰鍛崇潃 report 宸茬粡鍙互鍚姩銆?

鍚姩鍓嶅彧鍐嶇‘璁よ繖 4 浠朵簨锛?

- `analysis` 宸茶兘绋冲畾鎻愪緵 `ana_result_summary`銆乣ana_visual_asset`銆乣med_risk_assessment_record`
- case 鐘舵€佹満鎺ュ彛鍙敤锛宺eport 涓嶅緱鐩存帴鏀?`med_case`
- MinIO/attachment 鍩虹鑳藉姏鍙敤锛屽洜涓?PDF 鏈€缁堣褰掓。鍒伴檮浠朵綋绯?
- system 鐨?org 闅旂鏌ヨ妯″紡宸茬粡鐢熸晥銆?

------

## 涓夈€佸凡鎷嶆澘鐨?report 妯″潡鍐崇瓥

### D1锛氬厛鍋氣€滄姤鍛婅褰?+ 鐗堟湰褰掓。鈥濓紝鍚庡仛鈥淧DF 缇庡寲鈥?

鍏堟妸涓婚摼璺窇閫氾紝涓嶈涓€寮€濮嬪氨鍦ㄦā鏉挎牱寮忎笂鑺卞お澶氭椂闂淬€?
 绗竴闃舵鍏堝仛鍒帮細

- 鑳界敓鎴?`rpt_record`
- 鑳藉尯鍒嗗尰鐢熺増 / 鎮ｈ€呯増
- 鑳介€掑 `version_no`
- 鑳借惤 `med_attachment`
- 鑳借 `rpt_export_log`

### D2锛氭姤鍛婄敓鎴愬繀椤昏蛋鐥呬緥瀛愯祫婧愯矾寰?

鍐荤粨鎺ュ彛宸茬粡缁欎簡锛?

- `POST /api/v1/cases/{caseId}/reports`
- `GET /api/v1/cases/{caseId}/reports`
- `POST /api/v1/reports/{reportId}/export`銆?

### D3锛歳eport 妯″潡绂佹缁曞紑 case 鐘舵€佹満

report 鐢熸垚瀹屾垚鍚庤嫢闇€瑕佹帹鍔ㄧ梾渚嬭繘鍏?`REPORT_READY`锛屽繀椤荤粺涓€閫氳繃 case 妯″潡鍏紑鎺ュ彛娴佽浆锛屼笉鑳藉湪 report 閲岀洿鎺?update `med_case.case_status_code`銆傝繖鍜屼綘鍒氬湪 analysis 妯″潡閲屽畧浣忕殑纭竟鐣屾槸涓€濂楀師鍒欍€?

------

## 鍥涖€佸紑鍙戦『搴?

## Step 1锛氬喕缁?report 妯″潡杈撳叆涓庤緭鍑?

鍏堟槑纭?report 鐢熸垚鏃惰鍙栧摢浜涙暟鎹紝閬垮厤杈瑰啓杈规紓銆?

### 鍖荤敓鐗堟姤鍛婃渶灏忚緭鍏?

- 鐥呬緥鍩虹淇℃伅
- 褰卞儚鍒楄〃
- AI 鎽樿缁撴灉
- 涓嶇‘瀹氭€ф彁绀?
- 椋庨櫓璇勪及缁撴灉
- 鍖荤敓淇/纭缁撴灉
- 鍖荤敓缁撹銆傚尰鐢熺増鎶ュ憡搴斿寘鍚梾渚嬫憳瑕併€佸奖鍍忓垪琛ㄣ€佺梾鐏跺畾浣嶃€佸垎绾х粨鏋溿€佷笉纭畾鎬ф彁绀恒€侀闄╄瘎浼般€佸鏍稿缓璁拰鍖荤敓缁撹銆?

### 鎮ｈ€呯増鎶ュ憡鏈€灏忚緭鍏?

- 閫氫織缁撴灉瑙ｉ噴
- 鍙兘寮傚父鐗欎綅
- 椋庨櫓绛夌骇鍗＄墖
- 澶嶆煡寤鸿
- 鏃ュ父鎶ょ悊寤鸿銆傛偅鑰呯増鎶ュ憡搴斿寘鍚€氫織瑙ｉ噴銆佸紓甯哥墮浣嶃€侀闄╃瓑绾с€佸鏌ュ缓璁拰鎶ょ悊寤鸿銆?

### 杈撳嚭

- `rpt_record`
- 褰掓。鍚庣殑 PDF attachment
- 瀵煎嚭鏃ュ織 `rpt_export_log`銆?

------

## Step 2锛氬厛鍋氭暟鎹ā鍨嬪拰鍒嗗眰楠ㄦ灦

鎸変綘浠浐瀹氬垎灞傛潵寤?`caries-report`锛?

```
caries-report/
鈹溾攢鈹€ controller/
鈹溾攢鈹€ app/
鈹溾攢鈹€ domain/
鈹?  鈹溾攢鈹€ model/
鈹?  鈹溾攢鈹€ service/
鈹?  鈹溾攢鈹€ event/
鈹?  鈹斺攢鈹€ repository/
鈹溾攢鈹€ infrastructure/
鈹?  鈹溾攢鈹€ mapper/
鈹?  鈹溾攢鈹€ repository/
鈹?  鈹溾攢鈹€ convert/
鈹?  鈹斺攢鈹€ client/
鈹斺攢鈹€ interfaces/
    鈹溾攢鈹€ dto/
    鈹溾攢鈹€ vo/
    鈹溾攢鈹€ query/
    鈹斺攢鈹€ command/
```

杩欐槸缁熶竴鍒嗗眰瑙勮寖锛屼笉瑕佹敼銆?

寤鸿绗竴鎵圭被灏卞缓杩欎簺锛?

### domain/model

- `ReportTemplateModel`
- `ReportRecordModel`
- `ReportExportLogModel`
- `ReportGenerateModel`

### domain/service

- `ReportDomainService`
- `ReportTemplateDomainService`

### domain/repository

- `ReportTemplateRepository`
- `ReportRecordRepository`
- `ReportExportLogRepository`

### app

- `ReportAppService`
- `ReportQueryAppService`

### controller

- `ReportController`

### interfaces/command

- `GenerateReportCommand`
- `ExportReportCommand`

### interfaces/vo

- `ReportDetailVO`
- `ReportListItemVO`
- `ReportExportResultVO`

------

## Step 3锛氬厛瀹炵幇 P5-1锛氭姤鍛婅褰?

浣犱滑浠诲姟娓呭崟閲?P5-1 宸茬粡寰堟槑纭細

- 鐢熸垚鎶ュ憡璁板綍
- `version_no` 閫掑
- 鍖荤敓鐗?/ 鎮ｈ€呯増鍒嗗瀷銆?

### 蹇呭仛瑙勫垯

1. 鍚屼竴鐥呬緥鍙湁澶氫唤鎶ュ憡
2. 鍚屼竴鐥呬緥鍚屼竴绫诲瀷鎶ュ憡鐢熸垚鏂扮増鏈椂锛屽彧鑳芥柊澧烇紝涓嶅彲瑕嗙洊鏃ц褰?
3. `version_no` 蹇呴』鎸?`(case_id, report_type_code)` 閫掑
4. 鎶ュ憡璁板綍蹇呴』甯?`org_id`
5. 鏌ヨ蹇呴』 org-aware銆?

### 杩欓噷鏈€鍏抽敭鐨勬柟娉?

```
ReportAppService.generateReport(caseId, GenerateReportCommand cmd)
```

寤鸿鍐呴儴姝ラ锛?

1. 鏌?case 鏄惁瀛樺湪涓斿綋鍓嶇敤鎴锋湁鏉冮檺
2. 鏌ョ梾渚嬪綋鍓嶇姸鎬佹槸鍚﹀厑璁哥敓鎴愭姤鍛?
3. 鑱氬悎鐥呬緥銆佸奖鍍忋€乤nalysis 鎽樿銆侀闄┿€佷慨姝ｇ粨鏋?
4. 璁＄畻褰撳墠 `reportTypeCode` 涓嬬殑鏂?`versionNo`
5. 鍏堣惤涓€鏉?`rpt_record`锛岀姸鎬佸厛璁?`DRAFT`
6. 璋冩ā鏉挎覆鏌?
7. 鐢熸垚 PDF 骞跺綊妗?attachment
8. 鍥炲啓 `rpt_record` 鐨勯檮浠跺紩鐢ㄥ拰鏈€缁堢姸鎬?
9. 閫氳繃 case 妯″潡鎶婄梾渚嬫帹杩涘埌 `REPORT_READY`銆侾5 鐨勭洰鏍囨湰韬氨鏄畬鎴愬尰鐢熺増/鎮ｈ€呯増鎶ュ憡鐢熸垚銆佺増鏈綊妗ｅ拰瀵煎嚭瀹¤銆?

------

## Step 4锛氬疄鐜?P5-2锛氭姤鍛婄敓鎴?

浣犱滑浠诲姟娓呭崟鍐欑殑鏄細

- 妯℃澘娓叉煋
- PDF 鐢熸垚
- attachment 褰掓。
- 鐘舵€佹洿鏂般€?

### 鎺ㄨ崘鍋氭硶

鍏堜笉瑕佽拷姹傚鏉傛ā鏉垮紩鎿庯紝绗竴鐗堢敤鏈€绋崇殑鏂瑰紡锛?

- HTML 妯℃澘
- 缁勮 `ReportRenderDTO`
- 娓叉煋 HTML
- 鍐嶈浆 PDF

### 浣犺鍏堝仛鐨?3 涓唴閮ㄧ粍浠?

- `ReportTemplateResolver`
- `ReportRenderService`
- `ReportPdfService`

### 妯℃澘绛栫暐

寤鸿鑷冲皯鍑嗗涓ゅ妯℃澘锛?

- `DOCTOR`
- `PATIENT`

鑰屼笖涓€寮€濮嬫ā鏉垮唴瀹规斁鍦?`rpt_template.template_content` 鎴栭」鐩祫婧愭枃浠堕兘琛岋紝浣嗚淇濊瘉鍚庣画鑳藉垏鎹㈡垚搴撹〃绠＄悊銆俙rpt_template` 鏈潵灏辨槸鎶ュ憡妯℃澘琛ㄣ€?

------

## Step 5锛氬疄鐜?P5-3锛氬鍑哄璁?

浠诲姟娓呭崟宸茬粡鍐欐槑锛?

- 瀵煎嚭鏃ュ織璁板綍
- 涓嬭浇鎿嶄綔瀹¤
- 鏉冮檺鏍￠獙銆?

瀵煎嚭鎺ュ彛涔熷凡缁忓喕缁擄細

- `POST /api/v1/reports/{reportId}/export`銆?

### 瀵煎嚭鎺ュ彛璇ュ仛浠€涔?

1. 鏍￠獙褰撳墠鐢ㄦ埛鏄惁鏈夎鎶ュ憡璁块棶鏉冮檺
2. 鏍￠獙鎶ュ憡鏄惁瀛樺湪涓?attachment 宸插綊妗?
3. 璁板綍涓€鏉?`rpt_export_log`
4. 杩斿洖瀵煎嚭鎴愬姛缁撴灉锛屾垨鑰呰繑鍥為绛惧悕涓嬭浇鍦板潃

### 娉ㄦ剰

瀵煎嚭涓嶆槸鈥滅洿鎺ョ粰鏂囦欢鈥濊繖涔堢畝鍗曘€?
 浣犱滑鏂囨。宸茬粡鎶婂璞″瓨鍌ㄥ畨鍏ㄨ姹傚喕缁撲簡锛?

- 榛樿 `PRIVATE`
- 瀵瑰璁块棶璧扮煭鏃堕绛惧悕
- 涓嶅厑璁稿墠绔洿鎷煎叕缃戝湴鍧€銆?

------

## Step 6锛欳ontroller 鎺ュ彛涓€娆℃€у畾濂?

鐩存帴鎸夊喕缁?API 鏉ワ紝涓嶈鑷繁鍐嶆敼椋庢牸锛?

### 1. 鐢熸垚鎶ュ憡

```
POST /api/v1/cases/{caseId}/reports
```

璇锋眰锛?

```
{
  "reportTypeCode": "DOCTOR"
}
```

杩斿洖锛?

```
{
  "reportId": 8001,
  "reportNo": "RPT202604110001",
  "reportTypeCode": "DOCTOR",
  "versionNo": 1,
  "reportStatusCode": "DRAFT"
}
```

杩欎釜濂戠害宸茬粡鍐荤粨銆?

### 2. 鐥呬緥鎶ュ憡鍒楄〃

`GET /api/v1/cases/{caseId}/reports`銆?

### 3. 瀵煎嚭鎶ュ憡

`POST /api/v1/reports/{reportId}/export`銆?

### 4. 寤鸿琛ヤ竴涓鎯呮帴鍙?

铏界劧鐗囨閲屾病鐩存帴鍐欐槑锛屼絾涓轰簡鈥滃垪琛ㄤ笌璇︽儏鍙煡鈥濈殑 P5 楠屾敹锛屽缓璁ˉ锛?
 `GET /api/v1/reports/{reportId}`銆侾5 楠屾敹鏄庣‘瑕佹眰鈥滄姤鍛婂垪琛ㄤ笌璇︽儏鍙煡鈥濄€?

------

## Step 7锛氬繀椤诲啓鐨勯鍩熻鍒?

杩欏潡鏈€瀹规槗琚拷瑙嗭紝浣嗗疄闄呮渶閲嶈銆?

### R1锛氬彧鏈夋弧瓒虫潯浠剁殑鐥呬緥鎵嶈兘鐢熸垚鎶ュ憡

寤鸿鑷冲皯瑕佹眰锛?

- 鐥呬緥瀛樺湪
- org 鏉冮檺鍚堟硶
- analysis 宸叉湁鍙秷璐圭粨鏋?
- 鍖荤敓宸茬‘璁わ紝鎴栨祦绋嬪厑璁哥洿鎺ュ嚭鎶ュ憡

### R2锛氱増鏈彿涓嶈兘闈犲墠绔紶

蹇呴』鍚庣鎸夋暟鎹簱璁板綍鑷閫掑銆?

### R3锛氬悓涓€浠芥姤鍛婁笉鍙瑕嗙洊

鏂板唴瀹?= 鏂扮増鏈紝涓嶆槸 update 鍘熺増銆?

### R4锛氭偅鑰呯増鍜屽尰鐢熺増鏄袱绫绘姤鍛?

涓嶈兘鐢ㄤ竴涓瓧娈垫枃鏈‖鎷兼贩杩囧幓銆備綘浠枃妗ｅ凡缁忔槑纭尰鐢熺増鍜屾偅鑰呯増鍐呭鑱岃矗涓嶅悓銆?

### R5锛歳eport 妯″潡鍙秷璐?analysis 缁撴灉锛屼笉鏀?analysis 缁撴灉

涓嶈鍦?report 閲屽弽鍚戜慨 analysis 琛ㄣ€
