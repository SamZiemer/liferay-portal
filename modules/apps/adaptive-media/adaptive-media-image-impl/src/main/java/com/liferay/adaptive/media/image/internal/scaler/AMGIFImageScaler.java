/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.adaptive.media.image.internal.scaler;

import com.liferay.adaptive.media.exception.AMRuntimeException;
import com.liferay.adaptive.media.image.configuration.AMImageConfigurationEntry;
import com.liferay.adaptive.media.image.internal.configuration.AMImageConfiguration;
import com.liferay.adaptive.media.image.internal.util.RenderedImageUtil;
import com.liferay.adaptive.media.image.internal.util.Tuple;
import com.liferay.adaptive.media.image.scaler.AMImageScaledImage;
import com.liferay.adaptive.media.image.scaler.AMImageScaler;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.awt.image.RenderedImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Sergio González
 */
@Component(
	configurationPid = "com.liferay.adaptive.media.image.internal.configuration.AMImageConfiguration",
	property = "mimeTypes=image/gif", service = AMImageScaler.class
)
public class AMGIFImageScaler implements AMImageScaler {

	@Override
	public boolean isEnabled() {
		return _amImageConfiguration.gifsicleEnabled();
	}

	@Override
	public AMImageScaledImage scaleImage(
		FileVersion fileVersion,
		AMImageConfigurationEntry amImageConfigurationEntry) {

		try {
			File file = _getFile(fileVersion);

			File destinationFile = FileUtil.createTempFile();

			_runGifsicleCommand(
				Arrays.asList(
					"gifsicle", "--resize-fit",
					_getResizeFitValues(amImageConfigurationEntry), "--output",
					destinationFile.getAbsolutePath(), file.getAbsolutePath()));

			file.delete();

			byte[] bytes = new byte[(int)destinationFile.length()];

			try (FileInputStream fileInputStream = new FileInputStream(
					destinationFile)) {

				fileInputStream.read(bytes);
			}

			Tuple<Integer, Integer> dimension = _getDimension(bytes);

			return new AMImageScaledImageImpl(
				bytes, dimension.second, fileVersion.getMimeType(),
				dimension.first);
		}
		catch (Exception exception) {
			throw new AMRuntimeException.IOException(exception);
		}
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_amImageConfiguration = ConfigurableUtil.createConfigurable(
			AMImageConfiguration.class, properties);
	}

	private void _consumeProcessInputStream(InputStream inputStream)
		throws IOException {

		BufferedReader bufferedReader = new BufferedReader(
			new InputStreamReader(inputStream));

		while (bufferedReader.ready()) {
			bufferedReader.readLine();
		}
	}

	private Tuple<Integer, Integer> _getDimension(byte[] bytes)
		throws IOException, PortalException {

		try (InputStream inputStream = new UnsyncByteArrayInputStream(bytes)) {
			RenderedImage renderedImage = RenderedImageUtil.readImage(
				inputStream);

			return Tuple.of(
				renderedImage.getWidth(), renderedImage.getHeight());
		}
	}

	private File _getFile(FileVersion fileVersion)
		throws IOException, PortalException {

		try (InputStream inputStream = fileVersion.getContentStream(false)) {
			return _file.createTempFile(inputStream);
		}
	}

	private String _getResizeFitValues(
		AMImageConfigurationEntry amImageConfigurationEntry) {

		Map<String, String> properties =
			amImageConfigurationEntry.getProperties();

		int maxHeight = GetterUtil.getInteger(properties.get("max-height"));

		String maxHeightString = StringPool.UNDERLINE;

		if (maxHeight != 0) {
			maxHeightString = String.valueOf(maxHeight);
		}

		int maxWidth = GetterUtil.getInteger(properties.get("max-width"));

		String maxWidthString = StringPool.UNDERLINE;

		if (maxWidth != 0) {
			maxWidthString = String.valueOf(maxWidth);
		}

		return StringBundler.concat(maxWidthString, "x", maxHeightString);
	}

	private void _runGifsicleCommand(List<String> gifsicleCommand)
		throws Exception {

		ProcessBuilder processBuilder = new ProcessBuilder(gifsicleCommand);

		processBuilder.redirectErrorStream(true);

		Process process = processBuilder.start();

		InputStream inputStream = process.getInputStream();

		while (true) {
			try {
				_consumeProcessInputStream(inputStream);

				if (!process.waitFor(5, TimeUnit.SECONDS)) {
					continue;
				}

				if (process.exitValue() != 0) {
					throw new Exception(
						StringBundler.concat(
							"Gifsicle command ",
							StringUtil.merge(gifsicleCommand, StringPool.SPACE),
							" failed with exit status ", process.exitValue()));
				}

				return;
			}
			catch (InterruptedException interruptedException) {
				if (_log.isDebugEnabled()) {
					_log.debug(interruptedException);
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AMGIFImageScaler.class);

	private volatile AMImageConfiguration _amImageConfiguration;

	@Reference
	private com.liferay.portal.kernel.util.File _file;

}