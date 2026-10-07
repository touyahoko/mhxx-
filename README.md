# MHXX 頑シミュ (Android版)

モンスターハンターダブルクロス 装備シミュレータのAndroid移植プロジェクトです。

元アプリ「頑張って作ったモンハンシミュレータ for MHXX ver.0.9」のデータを使用しています。

## 現状

| 機能 | 状態 |
|------|------|
| データ読み込み (装備/スキル/装飾品/お守り) | ✅ 実装済み |
| タブUI (元アプリ準拠) | ✅ 実装済み |
| スキル一覧・選択 | ✅ 実装済み |
| 装備一覧表示 | ✅ 実装済み |
| 装飾品・お守り一覧 | ✅ 実装済み |
| 装備組み合わせ探索 | ⏳ 未移植 (今後) |
| 除外/固定装備の反映 | ⏳ 未移植 |
| マイセット保存 | ⏳ 未移植 |

## ビルド方法

### ローカル (Android Studio)

1. Android Studio Hedgehog (2023.1.1) 以降を用意
2. このリポジトリを Clone / Open
3. Gradle Sync
4. Run (実機またはエミュレータ)

### GitHub Actions

`main` / `master` への push、または手動実行でデバッグAPKがビルドされます。
Artifacts から `app-debug.apk` をダウンロードできます。

### コマンドライン

```bash
# Gradle Wrapper がある場合
./gradlew assembleDebug

# 出力先
# app/build/outputs/apk/debug/app-debug.apk
```

## プロジェクト構成

```
app/src/main/
├── assets/
│   ├── data/          # MHXX_*.csv
│   └── conf/          # CATEGORY.txt 等
├── java/com/mhxx/gansimu/
│   ├── data/          # DataRepository (データ読み込み)
│   ├── model/         # Skill, Equipment, Decoration, Charm
│   └── ui/            # MainActivity + 各タブFragment
└── res/
```

## 元データについて

- 装備・スキル等のCSVは元シミュレータに付属していたものです
- Shift_JIS エンコーディングで読み込んでいます
- 元アプリの探索アルゴリズム (難読化されたJavaコード) の移植は今後の課題です

## ライセンス・注意

- 元シミュレータの作者に敬意を表します
- 本移植は学習・個人利用を目的としています
- ゲームデータは Capcom の著作物です

## 今後の移植予定

1. 装備組み合わせ探索ロジックの移植 (元 `c.g` 等)
2. 除外装備・固定装備の検索への反映
3. 装飾品の自動配置
4. マイセットの保存/読込 (SharedPreferences or Room)
5. UIのブラッシュアップ
