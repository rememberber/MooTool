mod commands;
mod contracts;
mod repositories;
mod state;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let _ = rustls::crypto::ring::default_provider().install_default();
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_autostart::init(
            tauri_plugin_autostart::MacosLauncher::LaunchAgent,
            Some(vec!["--mootool-autostart"]),
        ))
        .plugin(tauri_plugin_clipboard_manager::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_updater::Builder::new().build())
        .manage(commands::desktop::DesktopLifecycle::default())
        .manage(state::ToolWebviewManager::default())
        .manage(commands::http::HttpRequestManager::default())
        .manage(commands::code_runtime::CodeExecutionManager::default())
        .manage(commands::translation::TranslationManager::default())
        .manage(commands::native_desktop::NativeDesktopManager::default())
        .manage(commands::network_tools::NetworkTaskManager::default())
        .manage(commands::pdf_files::PdfExportManager::default())
        .manage(repositories::vault::VaultRepository::default())
        .manage(commands::vault::VaultGitManager::default())
        .manage(commands::update::ProductUpdateManager::default())
        .menu(commands::desktop::build_application_menu)
        .on_menu_event(commands::desktop::handle_menu_event)
        .on_tray_icon_event(commands::desktop::handle_tray_event)
        .on_window_event(commands::desktop::handle_window_event)
        .setup(|app| {
            use tauri::Manager;

            let native_acceptance = commands::native_acceptance::configuration_from_environment()?;
            let logging_directory = native_acceptance
                .as_ref()
                .map(|config| config.data_root.join("logs"));
            let logging =
                commands::diagnostics::initialize_logging(app, logging_directory.as_deref())?;
            app.manage(logging);
            let settings_path = match &native_acceptance {
                Some(config) => config
                    .data_root
                    .join("config")
                    .join(repositories::settings::SETTINGS_FILE_NAME),
                None => app
                    .path()
                    .app_config_dir()
                    .map_err(|error| {
                        format!("failed to resolve Tauri settings directory: {error}")
                    })?
                    .join(repositories::settings::SETTINGS_FILE_NAME),
            };
            let repository = repositories::settings::SettingsRepository::open(settings_path)
                .map_err(|error| format!("failed to initialize settings: {error}"))?;
            let settings_snapshot = repository.snapshot();
            app.manage(repository);
            commands::vault::configure_from_settings(
                app.handle(),
                app.state::<repositories::vault::VaultRepository>().inner(),
                &settings_snapshot,
            );
            let database_path = match &native_acceptance {
                Some(config) => config
                    .data_root
                    .join("data")
                    .join(repositories::local_data::DATABASE_FILE_NAME),
                None => app
                    .path()
                    .app_data_dir()
                    .map_err(|error| format!("failed to resolve Tauri data directory: {error}"))?
                    .join(repositories::local_data::DATABASE_FILE_NAME),
            };
            let local_data = repositories::local_data::LocalDataRepository::open(database_path)
                .map_err(|error| format!("failed to initialize local data: {error}"))?;
            app.manage(local_data);
            let window_state_directory = native_acceptance
                .as_ref()
                .map(|config| config.data_root.join("config"));
            commands::desktop::setup(
                app,
                window_state_directory.as_deref(),
                native_acceptance.is_none(),
            )?;
            if let Some(config) = native_acceptance {
                commands::native_acceptance::start(app.handle().clone(), config);
            }
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            commands::runtime::get_runtime_info,
            commands::diagnostics::get_environment_variables,
            commands::diagnostics::get_system_snapshot,
            commands::diagnostics::report_frontend_error,
            commands::diagnostics::export_diagnostics_bundle,
            commands::update::check_for_product_update,
            commands::update::install_product_update,
            commands::update::cancel_product_update,
            commands::update::relaunch_after_product_update,
            commands::vault::get_vault_snapshot,
            commands::vault::configure_vault,
            commands::vault::disconnect_vault,
            commands::vault::read_vault_document,
            commands::vault::save_vault_document,
            commands::vault::create_vault_directory,
            commands::vault::move_vault_entry,
            commands::vault::duplicate_vault_document,
            commands::vault::delete_vault_document,
            commands::vault::delete_vault_entry,
            commands::vault::get_vault_git_status,
            commands::vault::get_vault_git_details,
            commands::vault::configure_vault_git_remote,
            commands::vault::run_vault_git,
            commands::vault::cancel_vault_git,
            commands::settings::get_settings,
            commands::settings::update_settings,
            commands::settings::reset_settings,
            commands::settings::open_settings_window,
            commands::desktop::resolve_close_request,
            commands::local_data::list_quick_notes,
            commands::local_data::save_quick_note,
            commands::local_data::delete_quick_note,
            commands::local_data::list_quick_note_folders,
            commands::local_data::save_quick_note_folder,
            commands::local_data::rename_quick_note_folder,
            commands::local_data::delete_quick_note_folder,
            commands::local_data::list_tool_favorites,
            commands::local_data::save_tool_favorite,
            commands::local_data::delete_tool_favorite,
            commands::local_data::list_quick_note_attachments,
            commands::local_data::import_quick_note_attachment,
            commands::local_data::export_quick_note_attachment,
            commands::local_data::delete_quick_note_attachment,
            commands::local_data::list_board_messages,
            commands::local_data::save_board_message,
            commands::local_data::delete_board_message,
            commands::local_data::list_host_profiles,
            commands::local_data::save_host_profile,
            commands::local_data::delete_host_profile,
            commands::local_data::list_translation_words,
            commands::local_data::save_translation_word,
            commands::local_data::delete_translation_word,
            commands::local_data::list_translation_history,
            commands::local_data::delete_translation_history,
            commands::local_data::clear_translation_history,
            commands::local_data::read_system_hosts,
            commands::local_data::write_system_hosts,
            commands::local_data::resolve_host,
            commands::network_tools::list_network_interfaces,
            commands::network_tools::resolve_network_host,
            commands::network_tools::scan_tcp_ports,
            commands::network_tools::scan_ipv4_range,
            commands::network_tools::cancel_network_task,
            commands::network_tools::query_network_whois,
            commands::network_tools::list_network_connections,
            commands::network_tools::flush_network_dns_cache,
            commands::network_tools::ping_network_host,
            commands::http::execute_http_request,
            commands::http::cancel_http_request,
            commands::http::list_saved_http_requests,
            commands::http::save_http_request,
            commands::http::delete_saved_http_request,
            commands::http::list_http_request_history,
            commands::http::delete_http_request_history,
            commands::http::clear_http_request_history,
            commands::user_files::pick_text_file,
            commands::user_files::export_text_file,
            commands::user_files::digest_user_file,
            commands::pdf_files::begin_pdf_export,
            commands::pdf_files::write_pdf_export_chunk,
            commands::pdf_files::finish_pdf_export,
            commands::pdf_files::cancel_pdf_export,
            commands::code_runtime::detect_code_runtimes,
            commands::code_runtime::run_code,
            commands::code_runtime::cancel_code_run,
            commands::translation::translate_text,
            commands::translation::cancel_translation,
            commands::images::list_image_assets,
            commands::images::save_image_asset,
            commands::images::import_image_files,
            commands::images::read_image_asset,
            commands::images::export_image_assets,
            commands::images::rename_image_asset,
            commands::images::delete_image_assets,
            commands::native_desktop::capture_display_images,
            commands::native_desktop::sample_screen_color,
            commands::native_desktop::get_display_sleep_status,
            commands::native_desktop::set_display_sleep_prevention,
            commands::history::record_operation_history,
            commands::history::list_operation_history,
            commands::history::delete_operation_history,
            commands::history::clear_operation_history,
            commands::backup::export_product_backup,
            commands::backup::import_product_backup,
            commands::product_import::preview_product_import,
            commands::product_import::run_product_import,
            commands::tool_webview::get_tool_webview_snapshot,
            commands::tool_webview::open_tool_webview,
            commands::tool_webview::update_tool_webview_bounds,
            commands::tool_webview::set_tool_webview_visible,
            commands::tool_webview::detach_tool_webview,
            commands::tool_webview::dock_tool_webview,
            commands::tool_webview::stress_tool_webview_reparent,
            commands::tool_webview::close_tool_webview,
            commands::tool_webview::report_tool_webview_session
        ])
        .build(tauri::generate_context!())
        .expect("failed to build MooTool Next Tauri");
    app.run(|app, event| match event {
        #[cfg(target_os = "macos")]
        tauri::RunEvent::Reopen { .. } => {
            if let Err(error) = commands::desktop::show_main_window(app) {
                eprintln!("MooTool Next Tauri dock restore failed: {error}");
            }
        }
        tauri::RunEvent::ExitRequested { .. } => commands::desktop::flush_window_state(app),
        _ => {}
    });
}
